package ro.redeul.google.go.util;

import com.intellij.ide.Bootstrap;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import ro.redeul.google.go.lang.psi.expressions.GoExpr;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteral;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralFunction;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.expressions.literals.composite.GoLiteralComposite;
import ro.redeul.google.go.lang.psi.expressions.primary.GoCallOrConvExpression;
import ro.redeul.google.go.lang.psi.toplevel.GoFunctionParameter;
import ro.redeul.google.go.lang.psi.typing.GoType;
import ro.redeul.google.go.lang.psi.typing.GoTypeName;
import ro.redeul.google.go.lang.psi.typing.GoTypePointer;
import ro.redeul.google.go.lang.psi.typing.GoTypeSlice;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: Toader Mihai Claudiu <mtoader@gmail.com>
 * <p/>
 * Date: Aug 19, 2010
 * Time: 4:18:54 AM
 */
public class GoUtil {

    /**
     * Gets the idea home directory.
     * Note: Copied over from IDEA Main class but the home directory doesn't seem to be properly made available to
     * plugins.
     *
     * @return The idea home directory
     */
    private static File getIdeaHomeDir() {
        URL url = Bootstrap.class.getResource("");
        if (url == null || !"jar".equals(url.getProtocol())) return null;

        String path = url.getPath();

        int start = path.indexOf("file:/");
        int end = path.indexOf("!/");
        if (start == -1 || end == -1) return null;

        String jarFileUrl = path.substring(start, end);

        try {
            File bootstrapJar = new File(new URI(jarFileUrl));
            return bootstrapJar.getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean testGoHomeFolder(String goRoot) {
        return goRoot != null
                && goRoot.trim().length() > 0 && new File(goRoot).isDirectory();
    }

    private final static Pattern RE_PACKAGE_TARGET =
            Pattern.compile("^TARG=([^\\s]+)\\s*$", Pattern.MULTILINE);

    /**
     * Returns a string if there is a TARG=xxx specified in the provided makefile and null if there is no such file.
     *
     * @param makefile the file we want to test (can be null)
     * @return the specified target or null
     */
    public static String getTargetFromMakefile(VirtualFile makefile) {
        if (makefile == null) {
            return null;
        }

        try {
            String content = new String(makefile.contentsToByteArray(), "UTF-8");

            Matcher matcher = RE_PACKAGE_TARGET.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            //
        }

        return null;
    }


    public static String getFuncDecParam(GoLiteralFunction goLiteralFunction) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("func(");

        int counter = 0;
        for (GoFunctionParameter parameter :
                goLiteralFunction.getParameters()) {
            if (counter != 0)
                stringBuilder.append(',');
            stringBuilder.append(parameter.getType().getText());
            counter++;
        }
        stringBuilder.append(')');
        counter = 0;
        for (GoFunctionParameter parameter :
                goLiteralFunction.getResults()) {
            if (counter == 0) {
                stringBuilder.append('(');
            } else {
                stringBuilder.append(',');
            }
            stringBuilder.append(parameter.getType().getText());
            counter++;
        }

        if (counter != 0)
            stringBuilder.append(')');

        return stringBuilder.toString();
    }

    /**
     * Helper method to generate function arguments type, based on param being passed to the function
     *
     * @param e -> parent -> GoCallOrConvExpression
     * @return the generated arugment list ex: arg0 int, arg1 string
     */
    public static String InspectionGenFuncArgs(PsiElement e) {
        StringBuilder stringBuilder = new StringBuilder();
        int arg = 0;
        for (GoExpr argument : ((GoCallOrConvExpression) e.getParent()).getArguments()) {
            if (arg != 0)
                stringBuilder.append(',');
            stringBuilder.append("arg").append(arg).append(" ");
            PsiElement firstChildExp = argument.getFirstChild();
            //Compositives Types
            if (firstChildExp instanceof GoLiteralIdentifier) {
                stringBuilder.append(((GoLiteral) firstChildExp).getType().name().toLowerCase());
            } else if (firstChildExp instanceof GoLiteralComposite || firstChildExp.getText().equals("&")) {

                GoType[] goTypes = argument.getType();
                if (goTypes.length > 0) {
                    GoType goType = goTypes[0];
                    if (goType instanceof GoTypeSlice) {
                        stringBuilder.append(((GoTypeSlice) goType).getPsiType().getText());
                    } else if (goType instanceof GoTypeName) {
                        if (firstChildExp.getText().equals("&"))
                            stringBuilder.append('*');
                        stringBuilder.append(((GoTypeName) goType).getPsiType().getText());

                    } else if (goType instanceof GoTypePointer) {
                        goType = ((GoTypePointer) goType).getTargetType();
                        stringBuilder.append('*');
                        stringBuilder.append(((GoTypeName) goType).getPsiType().getText());
                    }
                } else {
                    stringBuilder.append("interface{}");
                }

            } else if (firstChildExp instanceof GoLiteralFunction) {
                stringBuilder.append(GoUtil.getFuncDecParam((GoLiteralFunction) firstChildExp));
            } else {
                stringBuilder.append(((GoLiteral) firstChildExp).getType().name().toLowerCase());
            }
            arg++;
        }

        return stringBuilder.toString();
    }
}

