package com.xmlcalabash.model;

import java.util.Set;

import net.sf.saxon.s9api.QName;

public interface DeclarationScope {
	public void declareStep(QName type, DeclareStep step);
	public DeclareStep getDeclaration(QName type);
	public Set<QName> getInScopeTypes();
}
