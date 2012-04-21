/*
 * Title:   jInterp.java
 * Author:  J.D. Manuel
 * Purpose: A simple command line "interpreter" for Java that immediately
 *          creates and compiles a .java file consisting of the command line
 *          input.  Statements are placed in an exec() method, which is then
 *          invoked, providing immediate execution of simple statements to test
 *          functions, etc.
 * 
 * Notes:   Code snippets from borrowed from both Terence Parr's github page
 *          at github.com/parrt and Accordess.com.  Both sites were also referenced
 *          heavily for assistance in using the Java Reflection API and dynamic
 *          compilation API, along with "Build a Reflection-Based Interpreter
 *          in Java" by Greg Travis, available on Devx.com at
 *          http://www.devx.com/Java/Article/7866/1954
 * 
 *          Code snippets taken are also noted in the comments above the
 *          methods that use them.
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import javax.tools.*;


public class jInterp
{   
    public String clString;    
    public String filename; 
    public String tmpdir = null;
    public static final String CLASSPATH = System.getProperty("java.class.path");
    
    public ClassLoader loader;
    private int counter;    
    
    public jInterp()
    {       
        this.counter = 0;
        this.tmpdir = System.getProperty("java.io.tmpdir"); 
        this.clString = "";
        this.filename = "";
        try {
            this.loader = new URLClassLoader
                    (new URL[]{new File(tmpdir).toURI().toURL()},
                    ClassLoader.getSystemClassLoader());
        } catch (MalformedURLException ex) {
            //Logger.getLogger(jInterp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args)
    {
        jInterp interp = new jInterp();
        interp.interpret();        
    }
    
    
    /* controlling function */    
    public void interpret()
    {        
        Scanner sc = new Scanner(System.in);
        System.out.print("> ");

        while (sc.hasNextLine())
        {
            System.out.print("> ");
            
            this.clString = sc.nextLine();
            if (this.counter == 0) {
                this.createSourceFile("import java.io.*; import java.util.*;"
                        + "public class Interp_" + this.counter + "{" + "public static "
                        + this.clString + "}");
            } else {
                this.createSourceFile("import java.io.*; import java.util.*;"
                        + "public class Interp_" + this.counter + " extends Interp_"
                        + (this.counter - 1) + "{" + "public static "
                        + this.clString + "}");
            }
            
            boolean compileOkay = this.createClass();

            if (compileOkay) {               
                this.counter++;
            } else {

                if (this.counter == 0) {
                    this.createSourceFile("import java.io.*; import java.util.*;"
                            + "public class Interp_" + this.counter + "{" +
                            "public static void exec()" + "{"
                            + this.clString + "}" + "}");
                } else {
                    this.createSourceFile("import java.io.*; import java.util.*;"
                            + "public class Interp_" + this.counter + " extends Interp_"
                            + (this.counter - 1) + "{" + "public static void exec()" + "{"
                            + this.clString + "}" + "}");
                }

                compileOkay = this.createClass();

                if (compileOkay) {
                    try {
                        this.execute("Interp_" + this.counter);
                    } catch (MalformedURLException ex) {
                        //Logger.getLogger(jInterp.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    this.counter++;
                } else {
                    System.out.println("Compilation failed. Syntax error.");
                }
            }
        }
    }
    
    
    /* interpret() helper functions */    
    private void createSourceFile(String line)
    {
        String name = "Interp_" + this.counter + ".java";
        
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(name));
            out.write(line);
            out.close();
        } catch (IOException e) {
            System.out.println("Source write failure: " + e);           
        }
         
        this.filename = name;        
    }
    
//   The code for this method was taken from Terence Parr's github page,
//   available at gibthub.com/parrt. Specifically, from BaseTest.java at
//   github.com/parrt/antlr4/blob/master/tool/test/org/antlr/v4/test/BaseTest.java.           
//   Used virtually unmodified, except that I only pass one file to the
//   FileManager invoked, and I also used a DiagnosticCollector. Example code
//   for the DiagnosticCollector was taken from Accordess.com, at
//   http://www.accordess.com/wpblog/an-overview-of-java-compilation-api-jsr-199/             
    private boolean createClass()
    {      
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();
        
        StandardJavaFileManager fileManager = compiler.getStandardFileManager
                (null, Locale.getDefault(), null);
        
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjects(filename);

        Iterable<String> compileOptions =
                Arrays.asList("-g", "-d", tmpdir, "-cp", tmpdir +
                System.getProperty("path.separator") + CLASSPATH);

        JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, diagnostics, compileOptions,
                null, compilationUnits);
        
        boolean ok = task.call();

        try {
            fileManager.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
        
        return ok;
    }
    
    /* The code for this method was also taken largely from Parr's BaseTest.java,
     * though modified for this class, and much of the output calls omitted.
     */    
    private void execute(String className) throws MalformedURLException
    {       
        Class<?> mainClass = null;
        
        try {
            mainClass = (Class<?>) loader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            //Logger.getLogger(jInterp.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {         
            Method[] methods = mainClass.getDeclaredMethods();           
            final Method mainMethod = mainClass.getDeclaredMethod("exec");           
            
            try {             
                mainMethod.invoke(null, null);                
                System.out.print("> ");             
            } catch (Exception ex) {
                //Logger.getLogger(jInterp.class.getName()).log(Level.SEVERE, null, ex);             
            }            
        } catch (Exception ex) {
            //Logger.getLogger(jInterp.class.getName()).log(Level.SEVERE, null, ex);           
        }        
    }   
}
