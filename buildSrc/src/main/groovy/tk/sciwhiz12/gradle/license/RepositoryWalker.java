package tk.sciwhiz12.gradle.license;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class RepositoryWalker {
    private RepositoryWalker() {
    }

    /**
     * Finds the last modified times of all files in the repository based off the Git commit information.
     *
     * <p>For files which are modified in the working tree but not yet committed, the last modified time is set to
     * the value of {@link Instant#now()} during invocation of this method. (The precise value of the instant in
     * relation to the start of the method invocation is unspecified.)</p>
     *
     * <p>There is no guarantee of the ordering of the returned map. If the map is needed to be in order, use
     * a {@link TreeMap}.</p>
     *
     * @param repository the git repository
     * @return a map of file paths relative to the repository working tree to their last modified times
     * @throws UncheckedIOException if any of the repository operations called by this method fails with an exception
     */
    public static Map<String, Instant> findLastModifiedTimes(Repository repository) {
        try (ObjectReader reader = repository.newObjectReader()) {

            final Set<String> unprocessed = new HashSet<>();
            final Map<String, Instant> fileToLastModified = new HashMap<>();

            // Walk the working tree recursively and list out all files into the unprocessed set
            try (TreeWalk treeWalk = new TreeWalk(repository, reader)) {
                treeWalk.addTree(new FileTreeIterator(repository));
                treeWalk.setRecursive(true);

                while (treeWalk.next()) {
                    unprocessed.add(treeWalk.getPathString());
                }
            }

            // Retrieve the HEAD commit
            // TODO: parameter to specify starting point?
            final RevCommit headCommit = repository.parseCommit(repository.resolve(Constants.HEAD));

            // Find uncommitted changes between working tree and HEAD commit, and
            // mark their last modified time as Instant#now()
            final Instant now = Instant.now();
            processUncommittedChanges(repository, headCommit, unprocessed, fileToLastModified, now);

            // Walk each commit starting from HEAD and going backwards in time
            try (RevWalk walker = new RevWalk(reader)) {
                walker.markStart(headCommit);

                // Track the previous commit; for the HEAD commit, this will be null but will be non-null for all others
                @Nullable RevCommit prevCommit = null;
                for (RevCommit commit : walker) {

                    if (prevCommit != null) {
                        // Walk the changes between the previous commit and the current commit, and mark their last
                        // modified time to the previous commit (since these changes are what was done in the previous
                        // commit; remember that we're walking backwards in time here)
                        try (TreeWalk treeWalk = new TreeWalk(repository, reader)) {
                            final Instant prevCommitTime = Instant.ofEpochMilli(prevCommit.getCommitTime() * 1000L);

                            treeWalk.addTree(prevCommit.getTree());
                            treeWalk.addTree(commit.getTree());
                            treeWalk.setRecursive(true);
                            treeWalk.setFilter(TreeFilter.ANY_DIFF);

                            while (treeWalk.next()) {
                                final String path = treeWalk.getPathString();
                                if (unprocessed.remove(path)) {
                                    fileToLastModified.put(path, prevCommitTime);
                                }
                            }
                        }
                    }

                    prevCommit = commit;
                    // Exit early if we have processed all files currently in the repository working tree
                    if (unprocessed.isEmpty()) {
                        break;
                    }
                }
            }

            // For repository files which aren't processed somehow by the above, default to Instant#now()
            for (String path : unprocessed) {
                fileToLastModified.put(path, now);
            }

            return fileToLastModified;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void processUncommittedChanges(Repository repository, RevCommit baseCommit, Set<String> unprocessed,
                                                  Map<String, Instant> fileToLastModified, Instant now) throws IOException {
        final IndexDiff diff = new IndexDiff(repository, baseCommit, new FileTreeIterator(repository));
        diff.diff();
        Set<String> uncommittedChanges = new HashSet<>();
        uncommittedChanges.addAll(diff.getAdded());
        uncommittedChanges.addAll(diff.getChanged());
        uncommittedChanges.addAll(diff.getRemoved());
        uncommittedChanges.addAll(diff.getMissing());
        uncommittedChanges.addAll(diff.getModified());
        uncommittedChanges.addAll(diff.getConflicting());
        uncommittedChanges.addAll(diff.getUntracked());
        for (String path : uncommittedChanges) {
            if (unprocessed.remove(path)) {
                fileToLastModified.put(path, now);
            }
        }
        Iterator<String> it = unprocessed.iterator();
        while (it.hasNext()) {
            String path = it.next();
            for (String folder : diff.getUntrackedFolders()) {
                if (path.startsWith(folder)) {
                    it.remove();
                    fileToLastModified.put(path, now);
                    break;
                }
            }
        }
    }
}
