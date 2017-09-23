package com.massfords.javac.effectivelyfinal;


import com.sun.source.util.JavacTask;

/**
 * Plugin that produces a compiler error if a method param is assigned to. This prevents the bad practice of assigning
 * to a param which should be treated as final (at least in Java) without having to add the final modifier to every
 * param.
 */
public class EffectivelyFinalPlugin implements com.sun.source.util.Plugin {

    @Override
    public String getName() {
        return "EffectivelyFinal";
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new EffectivelyFinalTaskListener(task));
    }
}
