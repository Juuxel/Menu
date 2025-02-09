package juuxel.menu.gradle;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AddNestedClasses extends DefaultTask {
    @InputFile
    public abstract RegularFileProperty getMinecraftJar();

    @OutputDirectory
    public abstract DirectoryProperty getMappingDir();

    @TaskAction
    public void run() throws IOException {
        try (var fs = FileSystems.newFileSystem(URI.create("jar:" + getMinecraftJar().get().getAsFile().toURI()), Map.of("create", false))) {
            var mappingTree = new MemoryMappingTree();
            var mappingDir = getMappingDir().get().getAsFile().toPath();
            MappingReader.read(mappingDir, MappingFormat.ENIGMA_DIR, mappingTree);

            boolean modified = false;
            boolean modifiedInThisPhase = true;
            Map<Path, List<Path>> childCache = new HashMap<>();

            while (modifiedInThisPhase) {
                modifiedInThisPhase = false;

                for (MappingTree.ClassMapping clas : List.copyOf(mappingTree.getClasses())) {
                    var clasName = clas.getSrcName();
                    var clasSimpleName = clasName.substring(clasName.lastIndexOf('/') + 1);
                    Path clasPath = fs.getPath(clas.getSrcName() + ".class");
                    var siblings = getChildren(childCache, clasPath.getParent());

                    for (Path sibling : siblings) {
                        var siblingSimpleName = sibling.getFileName().toString();
                        siblingSimpleName = siblingSimpleName.substring(0, siblingSimpleName.length() - ".class".length());
                        if (siblingSimpleName.startsWith(clasSimpleName + "$")) {
                            var lastPart = siblingSimpleName.substring(clasSimpleName.length());
                            var childName = clasName + lastPart;
                            if (mappingTree.getClass(childName) == null) {
                                modified = true;
                                modifiedInThisPhase = true;
                                var dstName = clas.getDstName(0) + lastPart;
                                mappingTree.addClass(new SimpleClassMapping(mappingTree, childName, dstName));
                            }
                        }
                    }
                }
            }

            if (modified) {
                try (var writer = MappingWriter.create(mappingDir, MappingFormat.ENIGMA_DIR)) {
                    mappingTree.accept(writer);
                }
            }
        }
    }

    private List<Path> getChildren(Map<Path, List<Path>> cache, Path dir) throws IOException {
        List<Path> result = cache.get(dir);
        if (result != null) return result;

        try (var children = Files.list(dir)) {
            result = children
                .filter(x -> x.toString().endsWith(".class"))
                .sorted()
                .toList();
        }

        return result;
    }

    private record SimpleClassMapping(MappingTree mappingTree, String srcName, String dstName) implements MappingTree.ClassMapping {
        @Override
        public Collection<? extends MappingTree.FieldMapping> getFields() {
            return List.of();
        }

        @Override
        public MappingTree.@Nullable FieldMapping getField(String field, @Nullable String desc) {
            return null;
        }

        @Override
        public MappingTree.FieldMapping addField(MappingTree.FieldMapping fieldMapping) {
            return null;
        }

        @Override
        public MappingTree.@Nullable FieldMapping removeField(String field, @Nullable String desc) {
            return null;
        }

        @Override
        public Collection<? extends MappingTree.MethodMapping> getMethods() {
            return List.of();
        }

        @Override
        public MappingTree.@Nullable MethodMapping getMethod(String method, @Nullable String desc) {
            return null;
        }

        @Override
        public MappingTree.MethodMapping addMethod(MappingTree.MethodMapping methodMapping) {
            return null;
        }

        @Override
        public MappingTree.@Nullable MethodMapping removeMethod(String method, @Nullable String desc) {
            return null;
        }

        @Override
        public MappingTree getTree() {
            return mappingTree;
        }

        @Override
        public void setDstName(String dstName, int ns) {
        }

        @Override
        public void setComment(String comment) {
        }

        @Override
        public String getSrcName() {
            return srcName;
        }

        @Override
        public @Nullable String getDstName(int ns) {
            return ns == 0 ? dstName : null;
        }

        @Override
        public @Nullable String getComment() {
            return null;
        }
    }
}
