package snapcodepro.app;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import snapcodepro.project.JavaData;
import snap.viewx.TextPage;
import snap.web.WebFile;

import java.util.Arrays;
import java.util.Set;

/**
 * A WebPage subclass to show class info for a .class file.
 */
public class ClassInfoPage extends TextPage {

    /**
     * Override to return class info text instead of class file contents.
     */
    protected String getDefaultText()
    {
        WebFile classFile = getFile();
        String jpath = classFile.getPath().replace(".class", ".java").replace("/bin/", "/src/");
        WebFile jfile = classFile.getSite().getFileForPath(jpath);
        JavaData javaData = jfile != null ? JavaData.getJavaDataForFile(jfile) : null;
        if (javaData == null)
            return "Class File not found";

        // Get decls and refs
        Set<JavaDecl> decls = javaData.getDecls();
        Set<JavaDecl> refs = javaData.getRefs();

        // Create StringBuffer and append Declarations
        StringBuffer sb = new StringBuffer();
        sb.append("\n    - - - - - - - - - - Declarations - - - - - - - - - -\n\n");
        JavaDecl[] declArray = decls.toArray(new JavaDecl[0]);
        Arrays.sort(declArray);

        // Iterate over decls
        for (JavaDecl decl : declArray) {

            // Print class
            if (decl instanceof JavaClass) {
                sb.append("Class ").append(decl.getFullName()).append('\n');

                // Iterate over decls
                for (JavaDecl d2 : declArray) {

                    // Print Members
                    if (d2 instanceof JavaMember) {
                        JavaMember member = (JavaMember) d2;
                        if (member.getDeclaringClass() == decl)
                            sb.append("    ").append(d2.getType()).append(' ').append(d2.getFullName()).append('\n');
                    }
                }
                sb.append('\n');
            }
        }

        // Append References
        sb.append("\n    - - - - - - - - - - References - - - - - - - - - -\n\n");
        JavaDecl[] refArray = refs.toArray(new JavaDecl[0]);
        Arrays.sort(refArray);

        // Iterate over refs
        for (JavaDecl ref : refArray)
            sb.append(ref.getType()).append(' ').append(ref.getFullName()).append('\n');

        // Set Text
        return sb.toString();
    }

}