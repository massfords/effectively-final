package com.massfords.javac.effectivelyfinal;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

/**
 * Listens for the end of the analyze phase to kick in and visit the compilation unit in search of violations of
 * the effectively final dogma.
 */
public class EffectivelyFinalTaskListener implements TaskListener {

    /**
     * visitor that traverses the code
     */
    private final EffectivelyFinalVisitor visitor;

    public EffectivelyFinalTaskListener(JavacTask task) {
        visitor = new EffectivelyFinalVisitor(task);
    }

    @Override
    public void started(TaskEvent e) {
        // don't care about the start event
    }

    /**
     * Kicks off the scan if we've just finished the analyze phase
     * @param e
     */
    @Override
    public void finished(TaskEvent e) {
        TaskEvent.Kind kind = e.getKind();
        if (kind == TaskEvent.Kind.ANALYZE) {
            // visit all of the classes to see if their method params are explicitly or effectively final
            CompilationUnitTree compilationUnit = e.getCompilationUnit();
            visitor.scan(compilationUnit, null);
        }
    }
}
