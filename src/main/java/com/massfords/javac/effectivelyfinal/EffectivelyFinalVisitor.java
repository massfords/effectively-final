package com.massfords.javac.effectivelyfinal;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor that traverses the compilation unit in search of assignments to params
 */
public class EffectivelyFinalVisitor extends TreePathScanner<Void, Set<Name>> {

    /**
     * Utilities for reporting errors and working with the task
     */
    private final Trees trees;

    /**
     * The compilation unit we're visiting.
     */
    private CompilationUnitTree compilationUnitTree;

    public EffectivelyFinalVisitor(JavacTask task) {
        trees = Trees.instance(task);
    }

    /**
     * Keeps track of the compilation unit since it's needed to report errors
     * @param node unit we're compiling
     * @param names set of non-final param names in scope
     */
    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Set<Name> names) {
        this.compilationUnitTree = node;
        return super.visitCompilationUnit(node, names);
    }

    /**
     * If we're visiting a method, we want to record any of its non-final params and if we have one
     * or more non-final param then we want to traverse into the body and hit every assignment.
     *
     * We're relying on javac to report any real errors and thus our logic here can be simpler.
     * For example, when visiting a method, we can ignore any context passed into us since any params in scope
     * from enclosing methods must already be explicitly or effectively final.
     *
     * Consider the following:
     *
     * void foo(int a, int b) {
     *
     *     MyClass mc = new MyClass() {
     *         void differentMethod(int c, int d) {
     *             a = c + d; // :--- we don't need to check for this since javac will catch for us
     *         }
     *     }
     * }
     *
     * Therefore, when we encounter a method like we have here, we can disregard the param given to us and
     * traverse into the body of the method with all of the non-final params from just this method.
     *
     * It's tempting to not traverse if all of the params are final or it has no params but there could
     * be something in the body like an inner class that has an assignment to one of its own params
     *
     * @param node method we're visiting
     * @param nonFinalParamsInScope params will be ignored since we'll reset when we scan from here
     */
    @Override
    public Void visitMethod(MethodTree node, Set<Name> nonFinalParamsInScope) {
        Set<Name> nonFinalParams = node.getParameters()
                .stream()
                .filter(varTree -> !varTree.getModifiers().getFlags().contains(Modifier.FINAL))
                .map(VariableTree::getName)
                .distinct()
                .collect(Collectors.toSet());

        scan(node.getBody(), nonFinalParams);
        return null;
    }

    /**
     * If we're assigning to an identifier then we see if this identifier has the same name as one of the
     * non-final params in scope. We only care about assignments to an identifier.
     *
     * Thus, a = 5; would be flagged by array[0] = "foo" would not be flagged. The left hand side of the assignment
     * operation must be an identifier in order for us to flag it.
     *
     * @param assignmentTree assignment AST node
     * @param nonFinalParamsInScope params to check against the LHS of the assignment
     */
    @Override
    public Void visitAssignment(AssignmentTree assignmentTree, Set<Name> nonFinalParamsInScope) {
        if (nonFinalParamsInScope != null && !nonFinalParamsInScope.isEmpty()) {
            ExpressionTree variable = assignmentTree.getVariable();
            variable.accept(new SimpleTreeVisitor<Void, Void>() {
                @Override
                public Void visitIdentifier(IdentifierTree node, Void aVoid) {
                    if (nonFinalParamsInScope.contains(node.getName())) {
                        // printing a message of type error counts as a compilation error
                        trees.printMessage(Diagnostic.Kind.ERROR,
                                String.format("EFFECTIVELY_FINAL: Assignment to param in `%s`", assignmentTree),
                                node, compilationUnitTree);
                    }
                    return null;
                }

            }, null);
        }
        return null;
    }
}
