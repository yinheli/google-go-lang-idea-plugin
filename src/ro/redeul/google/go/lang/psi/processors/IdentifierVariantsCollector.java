package ro.redeul.google.go.lang.psi.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.BaseScopeProcessor;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.Nullable;
import ro.redeul.google.go.GoIcons;
import ro.redeul.google.go.lang.completion.insertHandler.FunctionInsertHandler;
import ro.redeul.google.go.lang.documentation.DocumentUtil;
import ro.redeul.google.go.lang.psi.declarations.GoConstDeclaration;
import ro.redeul.google.go.lang.psi.declarations.GoVarDeclaration;
import ro.redeul.google.go.lang.psi.expressions.literals.GoLiteralIdentifier;
import ro.redeul.google.go.lang.psi.toplevel.GoFunctionDeclaration;
import ro.redeul.google.go.lang.psi.toplevel.GoMethodDeclaration;

import javax.swing.Icon;

/**
 * Author: Toader Mihai Claudiu <mtoader@gmail.com>
 * <p/>
 * Date: 5/22/11
 * Time: 8:35 PM
 */
public class IdentifierVariantsCollector extends BaseScopeProcessor{

    static final String builtInFunctions[] = {
            "append", "cap", "close", "complex", "copy", "imag", "len", "make", "new", "panic", "print", "println", "real", "recover"
    };

    List<LookupElement> variants = new ArrayList<LookupElement>();
    Set<String> names = new HashSet<String>();

    @Override
    public boolean execute(PsiElement element, ResolveState state) {

        if ( element instanceof GoFunctionDeclaration && ! (element instanceof GoMethodDeclaration) ) {
            collectFunctionName((GoFunctionDeclaration) element, state);
            return true;
        }

        if ( element instanceof GoVarDeclaration) {
            collectVariableDeclaration((GoVarDeclaration) element, state);
            return true;
        }

        if ( element instanceof GoConstDeclaration) {
            collectVariableDeclaration((GoConstDeclaration) element, state);
            return true;
        }

        return true;
    }

    private void collectVariableDeclaration(GoVarDeclaration declaration, ResolveState state) {

        GoLiteralIdentifier identifiers[] = declaration.getIdentifiers();

        boolean isImported = isImported(declaration, state);

        for (GoLiteralIdentifier identifier : identifiers) {
            if ( ! isImported || GoNamesUtil.isPublicType(identifier.getName()) ) {
                addVariant(identifier, identifier.getName(), state, PlatformIcons.VARIABLE_ICON);
            }
        }
    }

    private void collectVariableDeclaration(GoConstDeclaration declaration, ResolveState state) {

        GoLiteralIdentifier identifiers[] = declaration.getIdentifiers();

        boolean isImported = isImported(declaration, state);

        for (GoLiteralIdentifier identifier : identifiers) {
            if ( ! isImported || GoNamesUtil.isPublicType(identifier.getName()) ) {
                addVariant(identifier, identifier.getName(), state, GoIcons.CONST_ICON);
            }
        }
    }

    private void collectFunctionName(GoFunctionDeclaration function, ResolveState state) {

        // include this if:
        //   - inside the same package
        //   - is a public name

        if ( function.isMain() ) {
            return;
        }

        if ( ! isImported(function, state) || GoNamesUtil.isPublicType(function.getFunctionName()) ) {
            String text = DocumentUtil.getFunctionPresentationText(function);
            addVariant(function, text, state, PlatformIcons.FUNCTION_ICON, new FunctionInsertHandler());
        }
    }

    private boolean isImported(PsiElement target, ResolveState state) {
        return !(state.get(GoResolveStates.IsOriginalFile) || state.get(GoResolveStates.IsOriginalPackage));
    }

    private void addVariant(PsiNamedElement target, String presentableText, ResolveState state, Icon icon) {
        addVariant(target, presentableText, state, icon, null);
    }

    private void addVariant(PsiNamedElement target, String presentableText, ResolveState state, Icon icon,
                            @Nullable InsertHandler<LookupElement> insertHandler) {
        boolean isImported = isImported(target, state);

        String visiblePackageName = state.get(GoResolveStates.VisiblePackageName);

        String lookupString = target.getName();
        if (lookupString == null || presentableText == null) {
            return;
        }

        if (isImported && visiblePackageName != null && visiblePackageName.length() > 0) {
            presentableText = visiblePackageName + "." + presentableText;
            lookupString = visiblePackageName + "." + lookupString;
        }

        if (!names.contains(presentableText)) {
            String type = isImported ? state.get(GoResolveStates.PackageName) : "<current>";
            variants.add(LookupElementBuilder.create(target, lookupString).setIcon(icon).setTypeText(type)
                    .setPresentableText(presentableText).setInsertHandler(insertHandler));
            names.add(presentableText);
        }
    }

    public Object[] references() {

        FunctionInsertHandler functionInsertHandler = new FunctionInsertHandler();
        for (String builtInType : builtInFunctions) {
            variants.add(
                    LookupElementBuilder.create(builtInType)
                            .setTypeText("builtin", true)
                            .setInsertHandler(functionInsertHandler)
                            .setBold()
            );
        }

        return variants.toArray(new Object[variants.size()]);
    }
}
