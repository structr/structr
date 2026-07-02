/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

// Single-file build tool (JEP 330):  java ModulePathPartitioner.java <libDir> <classpathDir> <seedFile>
//
// Splits a flat directory of runtime dependency jars (libDir, produced by copy-dependencies) into a
// JPMS *module path* (jars kept in libDir) and a *class-path island* dir (jars moved to classpathDir).
// Classification = a curated, edition-independent SEED plus an automated pass that catches additional
// JPMS-hostile jars, so each edition's extra dependencies self-classify without hand-curated globs.
//
// Seed file (one rule per line; '#' comments; blank lines ignored). A rule matches a jar by file-name
// glob, or by its derived module name when prefixed 'name:'. Prefix '+' pins to the module path (wins):
//     <glob>            force this jar to the CLASS PATH (quarantine)         e.g.  neo4j-*.jar
//     name:<module>     force the jar with this module name to the CLASS PATH e.g.  name:annotations
//     +<glob>           force this jar to stay on the MODULE PATH             e.g.  +neo4j-java-driver-*.jar
//     +name:<module>    force the jar with this module name to the MODULE PATH
// '*' in a glob matches any characters. Module-name matching is version-agnostic.
//
// Automated pass (only ADDS to the class path; never moves a module-path-pinned jar):
//   1. invalid / underivable automatic-module name
//   2. duplicate module name across two module-path jars
//   3. split package (a package present in two module-path jars)
//   4. unsatisfiable HARD (non-static) requires (a modular jar needs an absent module)
// Conflict offenders (2/3) = members not pinned to the module path, dropped worst-first (most packages,
// i.e. uber jars, then by name) while a conflict remains — deterministic.

import java.io.*;
import java.lang.module.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class ModulePathPartitioner {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) { System.err.println("usage: <libDir> <classpathDir> <seedFile>"); System.exit(2); }
        final Path libDir = Path.of(args[0]);
        final Path cpDir  = Path.of(args[1]);
        final Path seed   = Path.of(args[2]);

        final List<Pattern> forceCpGlob = new ArrayList<>(), forceMpGlob = new ArrayList<>();
        final Set<String>   forceCpName = new HashSet<>(),  forceMpName = new HashSet<>();
        for (String raw : Files.readAllLines(seed)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            boolean mp = line.startsWith("+");
            if (mp) line = line.substring(1).strip();
            if (line.startsWith("name:")) { (mp ? forceMpName : forceCpName).add(line.substring(5).strip()); }
            else                          { (mp ? forceMpGlob : forceCpGlob).add(glob(line)); }
        }

        Files.createDirectories(cpDir);
        final List<String> moved = new ArrayList<>();

        // derive a module name per jar up-front (null if not derivable)
        Map<Path,String> nameByJar = new LinkedHashMap<>();
        for (Path jar : jars(libDir)) nameByJar.put(jar, moduleName(jar));

        // helpers that consult both the file name and the derived module name
        java.util.function.Predicate<Path> pinnedMp = jar -> {
            String fn = jar.getFileName().toString(); String mn = nameByJar.get(jar);
            return matches(forceMpGlob, fn) || (mn != null && forceMpName.contains(mn));
        };

        // ----- pass 0: curated seed -----
        for (Path jar : jars(libDir)) {
            if (pinnedMp.test(jar)) continue;
            String fn = jar.getFileName().toString(); String mn = nameByJar.get(jar);
            if (matches(forceCpGlob, fn) || (mn != null && forceCpName.contains(mn))) move(jar, cpDir, moved, "seed");
        }

        // ----- automated pass over what remains on the module path -----
        Map<Path,ModuleDescriptor> desc = new LinkedHashMap<>();
        for (Path jar : jars(libDir)) {
            try { desc.put(jar, ModuleFinder.of(jar).findAll().iterator().next().descriptor()); }
            catch (FindException | InvalidModuleDescriptorException e) {
                if (!pinnedMp.test(jar)) move(jar, cpDir, moved, "invalid-module-name");  // rule 1
            }
        }

        // rule 0: a jar that contains a package owned by a JDK/system module (e.g. xml-apis / an uber
        // jar bundling javax.xml.* or org.w3c.dom.*) shadows that system module -> quarantine.
        Set<String> systemPkgs = ModuleFinder.ofSystem().findAll().stream()
                .flatMap(r -> r.descriptor().packages().stream()).collect(Collectors.toSet());
        for (var e : new ArrayList<>(desc.entrySet())) {
            if (pinnedMp.test(e.getKey())) continue;
            if (e.getValue().packages().stream().anyMatch(systemPkgs::contains)) {
                move(e.getKey(), cpDir, moved, "splits-system-module");
                desc.remove(e.getKey());
            }
        }

        // rule 4: unsatisfiable hard requires
        Set<String> present = desc.values().stream().map(ModuleDescriptor::name).collect(Collectors.toCollection(HashSet::new));
        Set<String> system  = ModuleFinder.ofSystem().findAll().stream().map(r -> r.descriptor().name()).collect(Collectors.toSet());
        for (var e : new ArrayList<>(desc.entrySet())) {
            if (pinnedMp.test(e.getKey())) continue;
            for (ModuleDescriptor.Requires r : e.getValue().requires()) {
                if (r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) continue;
                if (!present.contains(r.name()) && !system.contains(r.name())) {
                    move(e.getKey(), cpDir, moved, "unsatisfiable-requires:" + r.name());
                    desc.remove(e.getKey());
                    break;
                }
            }
        }

        // rules 2 + 3: duplicate module name / split package
        Map<String,List<Path>> byName = new LinkedHashMap<>(), byPkg = new LinkedHashMap<>();
        for (var e : desc.entrySet()) {
            byName.computeIfAbsent(e.getValue().name(), k -> new ArrayList<>()).add(e.getKey());
            for (String p : e.getValue().packages()) byPkg.computeIfAbsent(p, k -> new ArrayList<>()).add(e.getKey());
        }
        // never quarantine a jar whose module is required (transitively) by a module-path module:
        // doing so would make that requirer unresolvable (e.g. structr.base requires org.dom4j).
        Set<String> required = new HashSet<>();
        for (ModuleDescriptor d : desc.values())
            for (ModuleDescriptor.Requires r : d.requires())
                if (!r.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) required.add(r.name());
        Comparator<Path> keepLast = Comparator    // keepers sort to the END (least likely to be moved)
                .<Path>comparingInt(j -> pinnedMp.test(j) ? 1 : 0)
                .thenComparingInt(j -> required.contains(desc.get(j).name()) ? 1 : 0)
                .thenComparingInt(j -> desc.get(j).packages().size())
                .thenComparing(j -> j.getFileName().toString());

        // rule 2: duplicate module name — two jars deriving the SAME module name cannot coexist on the
        // module path; keep exactly one (prefer pinned, then required, then most packages) and move the rest.
        for (var en : new ArrayList<>(byName.entrySet())) {
            if (en.getValue().size() < 2) continue;
            List<Path> grp = en.getValue().stream().sorted(keepLast).collect(Collectors.toList());
            for (Path jar : grp.subList(0, grp.size() - 1)) {       // all but the last (the keeper)
                if (pinnedMp.test(jar)) continue;                   // cannot move a pinned jar
                move(jar, cpDir, moved, "duplicate-module-name:" + en.getKey());
                desc.remove(jar);
            }
        }

        // rule 3: split package across DIFFERENT modules — move the non-pinned, non-required member(s),
        // worst-first (uber jars), while a conflict remains.
        Set<Path> conflicting = new LinkedHashSet<>();
        for (var en : byPkg.entrySet()) if (en.getValue().size() > 1) conflicting.addAll(en.getValue());
        List<Path> offenders = conflicting.stream()
                .filter(desc::containsKey)
                .filter(j -> !pinnedMp.test(j) && !required.contains(desc.get(j).name()))
                .sorted(Comparator.<Path>comparingInt(j -> -desc.get(j).packages().size())
                        .thenComparing(j -> j.getFileName().toString()))
                .collect(Collectors.toList());
        for (Path jar : offenders) {
            if (!stillConflicts(jar, desc)) continue;
            move(jar, cpDir, moved, "split-package");
            desc.remove(jar);
        }

        System.out.println("[partitioner] quarantined " + moved.size() + " jar(s) to " + cpDir.getFileName()
                + "; " + jars(libDir).size() + " remain on the module path");
        for (String m : moved) System.out.println("    -> " + m);
    }

    private static String moduleName(Path jar) {
        try { return ModuleFinder.of(jar).findAll().iterator().next().descriptor().name(); }
        catch (RuntimeException e) { return null; }
    }
    private static boolean stillConflicts(Path jar, Map<Path,ModuleDescriptor> desc) {
        ModuleDescriptor me = desc.get(jar); if (me == null) return false;
        for (var e : desc.entrySet()) {
            if (e.getKey().equals(jar)) continue;
            if (e.getValue().name().equals(me.name())) return true;
            for (String p : me.packages()) if (e.getValue().packages().contains(p)) return true;
        }
        return false;
    }
    private static List<Path> jars(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().collect(Collectors.toList());
        }
    }
    private static void move(Path jar, Path cpDir, List<String> moved, String why) throws IOException {
        Files.move(jar, cpDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        moved.add(jar.getFileName() + "  (" + why + ")");
    }
    private static Pattern glob(String g) {
        StringBuilder b = new StringBuilder("^");
        for (char c : g.toCharArray()) {
            if (c == '*') b.append(".*");
            else if ("\\.[]{}()+-?^$|".indexOf(c) >= 0) b.append('\\').append(c);
            else b.append(c);
        }
        return Pattern.compile(b.append("$").toString());
    }
    private static boolean matches(List<Pattern> pats, String name) {
        for (Pattern p : pats) if (p.matcher(name).matches()) return true;
        return false;
    }
}
