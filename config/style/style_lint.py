#!/usr/bin/env python3
"""Structr semantic blank-line linter.

Checks the house-style blank-line rules that no off-the-shelf formatter can express
(see CODE_STYLE.md). Two modes:

    style_lint.py --check <paths...>    report violations, exit 1 if any
    style_lint.py --fix   <paths...>    apply the deterministic fixes in place

Rules:
  R1a method/type multi-statement body -> blank line after '{'         (fix: insert)
  R1b one-line method body stays tight -> no blank after '{'           (fix: remove)
  R4  every control block (if/for/while/try/…) -> blank after '{'      (fix: insert)
  R3  guard bound to a SINGLE preceding assignment -> no blank before  (fix: remove;
      precedence-aware, so a 2+ assignment group is left untouched)
  R2  consecutive single-line assignments grouped, no blank between    (fix: remove)
  RB  never more than one consecutive blank line                       (fix: collapse)
  R5  a returning statement preceded by code -> blank line before it   (report-only, fuzzy)

Every fix only inserts or deletes BLANK lines, so it can never change behaviour. R5 stays
report-only because deciding where a mid-method return wants its blank needs human judgement.
"""
import os, re, sys

CTRL_OPEN  = re.compile(r'^\s*(\}\s*)?(else if|else|if|for|while|catch|synchronized|try|do|finally)\b')
TYPE_OPEN  = re.compile(r'\b(class|interface|enum|record)\b')
NULL_EMPTY = re.compile(r'(==\s*null|!=\s*null|null\s*==|null\s*!=|\.isEmpty\(\)|\bisBlank\(|\bisNotBlank\(|\bisEmpty\(|\bisNotEmpty\()')
ASSIGN     = re.compile(r'^\s*(final\s+)?[\w.$]+(\s*<[^;=]*>)?(\[\])?\s+\w+\s*=[^=]')
EXIT       = re.compile(r'^\s*(return|continue|break|throw)\b')

def java_files(paths):
    for p in paths:
        if os.path.isfile(p) and p.endswith('.java'):
            yield p
        else:
            for dp, _, fns in os.walk(p):
                for fn in fns:
                    if fn.endswith('.java'):
                        yield os.path.join(dp, fn)

def match_body(L, i):
    s = L[i].rstrip()
    depth = s.count('{') - s.count('}')
    j = i + 1; body = 0
    while j < len(L) and depth > 0:
        depth += L[j].count('{') - L[j].count('}')
        if depth > 0 and L[j].strip():
            body += 1
        j += 1
    return body

def blank(L, i):
    return 0 <= i < len(L) and L[i].strip() == ''

def scan(L):
    """Return (insert_after, remove, reports): sets of 0-based line indices + (idx, rule) list."""
    insert_after, remove, reports = set(), set(), []
    for i, raw in enumerate(L):
        s = raw.rstrip()

        if EXIT.match(raw) and raw.strip().startswith('return') and i > 0:
            prev = L[i-1].rstrip()
            if prev.strip() and not prev.endswith('{') and prev.strip() != '}':
                reports.append((i, 'R5 blank line missing before return'))

        if not s.endswith('{'):
            continue

        is_ctrl = bool(CTRL_OPEN.match(raw)) and not re.search(r'\bswitch\s*\(', s)
        is_type = bool(TYPE_OPEN.search(s)) and '(' not in s.split('{')[0]
        is_method = ('(' in s and ')' in s and '->' not in s and not is_ctrl
                     and 'new ' not in s and not raw.lstrip().startswith('@')
                     and not re.search(r'\bswitch\s*\(', s))
        body = match_body(L, i)
        nxt_blank = blank(L, i+1)

        if is_method or is_type:
            if body >= 2 and not nxt_blank:
                insert_after.add(i); reports.append((i, 'R1a blank line missing after method/type "{"'))
            elif body == 1 and nxt_blank:
                remove.add(i+1); reports.append((i, 'R1b one-line body should be tight (stray blank after "{")'))
        elif is_ctrl:
            if body >= 1 and not nxt_blank:
                insert_after.add(i); reports.append((i, 'R4 blank line missing after control-block "{"'))
            if NULL_EMPTY.search(s):
                has_blank_before = blank(L, i-1)
                j = i - 2 if has_blank_before else i - 1
                run = 0
                while j >= 0 and ASSIGN.match(L[j]):
                    run += 1; j -= 1
                if run == 1 and has_blank_before:
                    remove.add(i-1); reports.append((i, 'R3 stray blank between assignment and bound guard'))

    # RB: collapse interior runs of 2+ blank lines (keep the first); leave any trailing/EOF run alone.
    run_start = None
    for i in range(len(L)):
        if L[i].strip() == '':
            if run_start is None:
                run_start = i
        else:
            if run_start is not None and i - run_start > 1:
                for k in range(run_start + 1, i):
                    remove.add(k); reports.append((k, 'RB more than one consecutive blank line'))
            run_start = None

    # R2: consecutive single-line declarations/assignments must not be separated by a blank.
    # Both neighbours must be complete single-line statements (end with ';') so multi-line
    # initialisers and non-declaration statements are never touched.
    for i in range(len(L) - 2):
        if (ASSIGN.match(L[i]) and L[i].rstrip().endswith(';')
                and L[i+1].strip() == ''
                and ASSIGN.match(L[i+2]) and L[i+2].rstrip().endswith(';')):
            remove.add(i + 1); reports.append((i + 1, 'R2 stray blank between grouped assignments'))

    return insert_after, remove, reports

def main():
    if len(sys.argv) < 3 or sys.argv[1] not in ('--check', '--fix'):
        print(__doc__); sys.exit(2)
    mode, paths = sys.argv[1], sys.argv[2:]
    total_reports = total_ins = total_del = 0
    for f in sorted(java_files(paths)):
        L = open(f, encoding='utf-8').read().split('\n')
        insert_after, remove, reports = scan(L)
        if mode == '--check':
            for idx, rule in sorted(reports):
                print(f"{f}:{idx+1}: {rule}")
            total_reports += len(reports)
        else:
            if insert_after or remove:
                out = []
                for idx, line in enumerate(L):
                    if idx in remove:
                        continue
                    out.append(line)
                    if idx in insert_after:
                        out.append('')
                open(f, 'w', encoding='utf-8').write('\n'.join(out))
                print(f"{f}: +{len(insert_after)} inserted, -{len(remove)} removed")
                total_ins += len(insert_after); total_del += len(remove)
    if mode == '--check':
        print(f"\n{total_reports} violation(s).")
        sys.exit(1 if total_reports else 0)
    print(f"\ntotal: +{total_ins} inserted, -{total_del} removed. Re-run --check for the report-only R5 rule.")

if __name__ == '__main__':
    main()
