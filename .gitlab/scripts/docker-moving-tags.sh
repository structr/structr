#!/usr/bin/env bash
#
# Maintain the moving Docker tags (MAJOR, MAJOR.MINOR, latest) for Structr releases.
#
# Retag only, never rebuild: "docker buildx imagetools create --prefer-index=false" with a
# single source performs a carbon copy of the source descriptor, so a moving tag ends up on
# the byte-identical manifest - same digest, same media type, same platforms, same
# attestations. The immutable MAJOR.MINOR.PATCH tag itself is built and pushed by the
# fabric8 docker-maven-plugin in the app module's pom (structr-app/pom.xml here,
# structr-app-enterprise/pom.xml in structr-enterprise).
#
# Source and target are ALWAYS in the same repository. Never copy across registries: the
# image in the GitLab registry is built with provenance attestations disabled (see
# structr-app/pom.xml) and must not receive Docker Hub's attestation manifests.
#
# Used by the docker-moving-tags job in .gitlab/.gitlab-ci.yml, by the job of the same name
# in structr-enterprise (which downloads this file from the release branch and runs it with
# --suffix -enterprise against the GitLab registry only), and by hand to apply the moving
# tags to a release that was published earlier.

set -euo pipefail

DEFAULT_REPOS="docker.io/structr/structr registry.structr.com/structr/structr"
DEFAULT_REFERENCE_REPO="docker.io/structr/structr"

# Remember whether the repositories were chosen explicitly: a suffixed (enterprise) run must
# never fall back to the community defaults above, which point at Docker Hub.
REPOS_GIVEN=false;          [ -n "${STRUCTR_IMAGE_REPOS:-}" ]    && REPOS_GIVEN=true
REFERENCE_REPO_GIVEN=false; [ -n "${STRUCTR_REFERENCE_REPO:-}" ] && REFERENCE_REPO_GIVEN=true

REPOS="${STRUCTR_IMAGE_REPOS:-$DEFAULT_REPOS}"
REFERENCE_REPO="${STRUCTR_REFERENCE_REPO:-$DEFAULT_REFERENCE_REPO}"
SUFFIX="${STRUCTR_TAG_SUFFIX:-}"
TAG_KINDS="major,minor,latest"
KNOWN_RELEASES=""
DRY_RUN=false
STRICT=false
VERBOSE=false
VERSION=""

usage() {
	cat <<'USAGE_EOF'
Usage: docker-moving-tags.sh [options] <version>

Applies the moving tags MAJOR, MAJOR.MINOR and latest to an already published release
image, by retagging its manifest in place. Nothing is built and no image content is
pushed - only additional tag names are created.

Arguments:
  <version>                   A final release, e.g. 7.0.0 (or 7.0.0-enterprise with
                              --suffix -enterprise). Anything else is skipped.

Options:
  --repos "<repo> <repo>"     Target repositories (host/path, no tag).
                              Default: $STRUCTR_IMAGE_REPOS or
                              "docker.io/structr/structr registry.structr.com/structr/structr"
                              Pass "" to resolve the tag set without touching a registry.
  --reference-repo <repo>     Repository whose tag list defines the release history.
                              Default: $STRUCTR_REFERENCE_REPO or docker.io/structr/structr
  --suffix <suffix>           Tag suffix of the image line, e.g. -enterprise. The version
                              must then be MAJOR.MINOR.PATCH<suffix>, only tags with that
                              suffix count as releases, and the moving tags become
                              MAJOR<suffix> and MAJOR.MINOR<suffix> (latest stays bare).
                              Requires --repos and --reference-repo (or their environment
                              variables), so a suffixed run never falls back to the Docker
                              Hub defaults. Default: $STRUCTR_TAG_SUFFIX or empty.
  --tags <kinds>              Comma separated: major, minor, latest. Default: all three.
  --known-releases "<list>"   Space/newline separated release list; skips the registry
                              query. For offline use and tests.
  --dry-run                   Resolve and print, push nothing.
  --strict                    Exit 1 instead of 0 when the version is not a release, or
                              when a source tag is missing in a target repository.
  -v, --verbose               Echo every registry command.
  -h, --help                  This text.

Environment:
  STRUCTR_IMAGE_REPOS, STRUCTR_REFERENCE_REPO,
  STRUCTR_TAG_SUFFIX                            Defaults for the options above.
  REGISTRY_USER, REGISTRY_PASSWORD              Only needed when --reference-repo is a
                                                registry that does not allow anonymous
                                                pulls. Pushes always use the ambient
                                                "docker login" credentials.

A moving tag is only applied when the given version is the highest published release of
its scope: MAJOR needs the highest release of that major line, MAJOR.MINOR the highest of
that minor line, and latest the highest release overall. So a later patch of an older line
can never steal "latest" from a newer major version.

Exit codes: 0 on success or a deliberate skip, 1 if at least one retag failed.

Examples:
  docker-moving-tags.sh --dry-run 7.0.0
  docker-moving-tags.sh 7.0.0
  docker-moving-tags.sh --tags major,minor 6.3.0
  docker-moving-tags.sh --suffix -enterprise \
      --repos registry.structr.com/structr/structr-enterprise \
      --reference-repo registry.structr.com/structr/structr-enterprise 7.0.0-enterprise
USAGE_EOF
}

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
log() { printf '%s\n' "$*"; }
run() { $VERBOSE && printf '+ %s\n' "$*" >&2; "$@"; }

# --- argument parsing --------------------------------------------------------------------

while [ $# -gt 0 ]; do
	case "$1" in
		--repos)           [ $# -ge 2 ] || die "--repos needs a value";           REPOS="$2";           REPOS_GIVEN=true;          shift 2 ;;
		--reference-repo)  [ $# -ge 2 ] || die "--reference-repo needs a value";  REFERENCE_REPO="$2";  REFERENCE_REPO_GIVEN=true; shift 2 ;;
		--suffix)          [ $# -ge 2 ] || die "--suffix needs a value";          SUFFIX="$2";          shift 2 ;;
		--tags)            [ $# -ge 2 ] || die "--tags needs a value";            TAG_KINDS="$2";       shift 2 ;;
		--known-releases)  [ $# -ge 2 ] || die "--known-releases needs a value";  KNOWN_RELEASES="$2";  shift 2 ;;
		--dry-run)         DRY_RUN=true;  shift ;;
		--strict)          STRICT=true;   shift ;;
		-v|--verbose)      VERBOSE=true;  shift ;;
		-h|--help)         usage; exit 0 ;;
		-*)                die "unknown option '$1' (try --help)" ;;
		*)                 [ -z "$VERSION" ] || die "unexpected argument '$1'"; VERSION="$1"; shift ;;
	esac
done

[ -n "$VERSION" ] || { usage >&2; exit 1; }

[[ "$SUFFIX" =~ ^[A-Za-z0-9._-]*$ ]] || die "--suffix: '${SUFFIX}' may only contain letters, digits, '.', '_' and '-'"
if [ -n "$SUFFIX" ]; then
	$REPOS_GIVEN && $REFERENCE_REPO_GIVEN \
		|| die "--suffix requires explicit --repos and --reference-repo - the defaults are the community repositories on Docker Hub"
fi
# The suffix as a regex fragment; "." is the only allowed character that is special there.
SUFFIX_RE="${SUFFIX//./\\.}"

for kind in ${TAG_KINDS//,/ }; do
	case "$kind" in
		major|minor|latest) ;;
		*) die "--tags: unknown kind '$kind' (expected major, minor or latest)" ;;
	esac
done

want() { case ",${TAG_KINDS}," in *",$1,"*) return 0 ;; *) return 1 ;; esac; }

command -v docker >/dev/null || die "docker not found"
command -v jq     >/dev/null || die "jq not found"

# --- registry helpers --------------------------------------------------------------------

# Docker Hub's own API, so the listing works without credentials on a public repository.
list_tags_dockerhub() {
	local path="$1" url page
	url="https://hub.docker.com/v2/repositories/${path}/tags/?page_size=100"
	while [ -n "$url" ] && [ "$url" != "null" ]; do
		page="$(curl -fsSL "$url")" || die "Docker Hub tag listing failed for ${path}"
		printf '%s\n' "$page" | jq -r '.results[].name'
		url="$(printf '%s\n' "$page" | jq -r '.next')"
	done
}

# Any other OCI registry: read the auth realm out of the 401 challenge on /v2/, fetch a
# pull token, then list. Works for registry.structr.com without hardcoding the GitLab host.
list_tags_registry_v2() {
	local host="$1" path="$2" challenge realm service token
	challenge="$(curl -sSI "https://${host}/v2/" | tr -d '\r' | grep -i '^www-authenticate:' || true)"
	realm="$(printf '%s' "$challenge"   | sed -n 's/.*realm="\([^"]*\)".*/\1/p')"
	service="$(printf '%s' "$challenge" | sed -n 's/.*service="\([^"]*\)".*/\1/p')"
	[ -n "$realm" ] || die "https://${host}/v2/ advertised no auth realm"
	token="$(curl -fsSL ${REGISTRY_USER:+-u "${REGISTRY_USER}:${REGISTRY_PASSWORD:-}"} \
		"${realm}?service=${service}&scope=repository:${path}:pull" \
		| jq -r '.token // .access_token // empty')" \
		|| die "could not obtain a pull token for ${host}/${path}"
	[ -n "$token" ] || die "empty pull token for ${host}/${path}"
	curl -fsSL -H "Authorization: Bearer ${token}" \
		"https://${host}/v2/${path}/tags/list?n=1000" | jq -r '.tags[]?' \
		|| die "tag listing failed for ${host}/${path}"
}

list_tags() {
	local repo="$1" host="${1%%/*}" path="${1#*/}"
	case "$host" in
		docker.io|index.docker.io|registry-1.docker.io) list_tags_dockerhub  "$path" ;;
		*)                                              list_tags_registry_v2 "$host" "$path" ;;
	esac
}

digest_of()  { docker buildx imagetools inspect --format '{{.Manifest.Digest}}' "$1" 2>/dev/null || true; }
tag_exists() { docker buildx imagetools inspect --raw "$1" >/dev/null 2>&1; }

# --- 1. release shape gate ---------------------------------------------------------------
# An allowlist, deliberately not a SNAPSHOT|rc|alpha|beta blocklist: this also rejects a
# future 7.1.0-rc1, which a naive blocklist would let through.

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+${SUFFIX_RE}$ ]]; then
	log "skip: '${VERSION}' is not a final release (expected MAJOR.MINOR.PATCH${SUFFIX}) - no moving tags"
	$STRICT && exit 1
	exit 0
fi

# All comparisons below work on the bare MAJOR.MINOR.PATCH; the suffix is only re-added to
# the tag names.
BASE="${VERSION%"$SUFFIX"}"
MAJOR="${BASE%%.*}"
MINOR_AND_PATCH="${BASE#*.}"
MINOR="${MINOR_AND_PATCH%%.*}"
SERIES="${MAJOR}.${MINOR}"

# --- 2. release history ------------------------------------------------------------------
# The version being released is folded in, so a momentarily stale listing cannot make the
# run wrong. A failed listing aborts: silently continuing with an incomplete history could
# move "latest" onto an older release. Only tags carrying exactly our suffix are releases of
# this image line, so community and enterprise releases never influence each other.

if [ -n "$KNOWN_RELEASES" ]; then
	raw_tags="$KNOWN_RELEASES"
else
	log "reading release history from ${REFERENCE_REPO}"
	raw_tags="$(list_tags "$REFERENCE_REPO")"
fi

releases="$(printf '%s\n%s\n' "${raw_tags// /$'\n'}" "$VERSION" \
	| grep -E "^[0-9]+\.[0-9]+\.[0-9]+${SUFFIX_RE}$" \
	| sed "s/${SUFFIX_RE}\$//" \
	| sort -V -u)"
[ -n "$releases" ] || die "no release versions found - refusing to guess"

highest_all="$(printf '%s\n'    "$releases" | tail -n 1)"
highest_major="$(printf '%s\n'  "$releases" | grep -E "^${MAJOR}\.[0-9]+\.[0-9]+$" | tail -n 1)"
highest_series="$(printf '%s\n' "$releases" | grep -E "^${SERIES}\.[0-9]+$"        | tail -n 1)"

# --- 3. decide the tag set ---------------------------------------------------------------

targets=()
consider() { # <kind> <tag> <highest-of-scope> <scope description>
	local kind="$1" tag="$2" highest="$3" scope="$4"
	want "$kind" || return 0
	if [ "$BASE" = "$highest" ]; then
		targets+=("$tag")
	else
		log "skip: ${tag} - ${VERSION} is not the highest ${scope} (${highest})"
	fi
}

consider major  "${MAJOR}${SUFFIX}"  "$highest_major"  "release of the ${MAJOR}.x line"
consider minor  "${SERIES}${SUFFIX}" "$highest_series" "release of the ${SERIES}.x line"
consider latest latest    "$highest_all"    "published release"

if [ "${#targets[@]}" -eq 0 ]; then
	log "nothing to do for ${VERSION}"
	exit 0
fi

log "moving tags for ${VERSION}: ${targets[*]}"
$DRY_RUN && log "(dry run - nothing will be pushed)"

# --- 4. apply ----------------------------------------------------------------------------

prefer_index=()
if docker buildx imagetools create --help 2>/dev/null | grep -q -- '--prefer-index'; then
	prefer_index=(--prefer-index=false)
else
	log "warn: this buildx has no --prefer-index; a single-platform source would be wrapped"
	log "warn: in a newly created index instead of being copied verbatim"
fi

dry_run_flag=()
$DRY_RUN && dry_run_flag=(--dry-run)

rc=0
for repo in $REPOS; do
	src="${repo}:${VERSION}"

	if ! tag_exists "$src"; then
		log "warn: ${src} not found - skipping ${repo}"
		$STRICT && rc=1
		continue
	fi

	src_digest="$(digest_of "$src")"
	log "== ${repo}: source ${VERSION} @ ${src_digest}"

	for tag in "${targets[@]}"; do
		dst="${repo}:${tag}"
		if [ "$(digest_of "$dst")" = "$src_digest" ]; then
			log "   ok    ${tag} already points at ${src_digest}"
			continue
		fi
		log "   move  ${tag} -> ${src_digest}"
		# The ${a[@]+"${a[@]}"} form keeps an empty array from tripping "set -u" on bash 3.2.
		if ! run docker buildx imagetools create \
			${prefer_index[@]+"${prefer_index[@]}"} ${dry_run_flag[@]+"${dry_run_flag[@]}"} \
			--tag "$dst" "$src"; then
			log "   FAIL  ${dst}"
			rc=1
		fi
	done
done

exit "$rc"
