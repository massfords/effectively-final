# Overview

It's best practice to not assign to a method parameter in the body of your method. You can find lots of opinions about
this but here's a good summary on [Stack Overflow](https://stackoverflow.com/questions/500508/why-should-i-use-the-keyword-final-on-a-method-parameter-in-java).

However, it's a bit tedious to have to write the modifier "final" before each parameter declaration and with the
addition of Java 8's "effecitvely final" concept it seems reasonable to relax the requirement of having the final
modifier on the parameter and instead move it to a compiler plugin. This is less typing and less clutter in your code.

## Explicitly final:

```java

public class ExplicitlyFinalExample {

    public void sendData(final String alpha, final String beta, final int gamma, final List<String> omega) {
        System.out.printf("sending %s, %s, %s, %s\n", alpha, beta, gamma, omega);
    }
}
```

## Effectively final:

```java

public class EffectivelyFinalExample {
    public void sendData(String alpha, String beta, int gamma, List<String> omega) {
        System.out.printf("sending %s, %s, %s, %s\n", alpha, beta, gamma, omega);
    }
}
```

In both snippets above, the method params are final. They are either final because they are explicitly declared
as being final or they are effectively final because they are never assigned to.

However, it's easy for the programmer to break this contract in the effectivley final example by assigning to one of
the method params in the body of the method. As long as this method parameter isn't used in an Anonymous Class or within
a lambda expression, it's still valid. This is exactly what this plugin is designed to catch during compilation.

## Breaking the effectively final contract:

```java

public class BreakingEffectivelyFinalExample {
    public void sendData(String alpha, String beta, int gamma, List<String> omega) {
    
        if (gamma < 100) {
            gamma = 100; // <|--- plugin will generate a compilation error on this line
        }
    
        System.out.printf("sending %s, %s, %s, %s\n", alpha, beta, gamma, omega);
    }
}
```

### Compilation Error

```
BreakingEffectivelyFinalExample.java:[5,13] error: EFFECTIVELY_FINAL: Assignment to param in `gamma = 100`
```

# Maven Config

You need to tell the maven compiler plugin to use this plugin during compilation. 

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.6.1</version>
    <configuration>
        <compilerArgs>
            <arg>-Xplugin:EffectivelyFinal</arg>
        </compilerArgs>
        <forceJavacCompilerUse>true</forceJavacCompilerUse>                
        </configuration>
    <dependencies>
        <dependency>
            <groupId>com.massfords</groupId>
            <artifactId>effectively-final</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>

```