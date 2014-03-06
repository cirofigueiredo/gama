/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.precompiler;

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.file;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.precompiler.GamlAnnotations.species;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.GamlAnnotations.type;
import msi.gama.precompiler.GamlAnnotations.var;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.precompiler.doc.DocProcessorAnnotations;
import msi.gama.precompiler.doc.Element.Category;
import msi.gama.precompiler.doc.Element.Operand;
import msi.gama.precompiler.doc.Element.Operator;
import msi.gama.precompiler.doc.utils.TypeConverter;
import msi.gama.precompiler.doc.utils.XMLElements;

import org.w3c.dom.*;

@SupportedAnnotationTypes({ "msi.gama.precompiler.GamlAnnotations.*" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GamlDocProcessor {

	public static final String BASIC_SKILL = "msi.gaml.skills.Skill";
	
	public static final Character[] cuttingLettersOperatorDoc = {'l'};

	ProcessingEnvironment processingEnv;
	Messager mes;
	TypeConverter tc;
	
	boolean firstParsing;

	// Statistiques values
	int nbrOperators;
	int nbrOperatorsDoc;
	int nbrSkills;
	int nbrSymbols;

	public GamlDocProcessor(final ProcessingEnvironment procEnv) {
		processingEnv = procEnv;
		mes = processingEnv.getMessager();
		firstParsing = true;
		nbrOperators = 0;
		nbrOperatorsDoc = 0;
		nbrSkills = 0;
		nbrSymbols = 0;
		tc = new TypeConverter();
	}

	public void processDocXML(final RoundEnvironment env, final Writer out) {

		DocumentBuilder docBuilder = null;

		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			System.err.println("Impossible to create a DocumentBuilder.");
			System.exit(1);
		}

		Document doc = docBuilder.newDocument();

		// Set<? extends Element> setRoot = env.getRootElements();
		org.w3c.dom.Element root = doc.createElement("doc");

		// ////////////////////////////////////////////////
		// /// Parsing of Operators Categories
		Set<? extends ExecutableElement> setOperatorsCategories =
			(Set<? extends ExecutableElement>) env.getElementsAnnotatedWith(operator.class);
		root.appendChild(this.processDocXMLOperatorsCategories(setOperatorsCategories, doc));

		// ////////////////////////////////////////////////
		// /// Parsing of Operators
		Set<? extends ExecutableElement> setOperators =
			(Set<? extends ExecutableElement>) env.getElementsAnnotatedWith(operator.class);
		root.appendChild(this.processDocXMLOperators(setOperators, doc));

		// ////////////////////////////////////////////////
		// /// Parsing of Skills
		Set<? extends Element> setSkills = env.getElementsAnnotatedWith(skill.class);
		root.appendChild(this.processDocXMLSkills(setSkills, doc));

		// ////////////////////////////////////////////////
		// /// Parsing of Species
		Set<? extends Element> setSpecies = env.getElementsAnnotatedWith(species.class);
		root.appendChild(this.processDocXMLSpecies(setSpecies, doc));

		// ////////////////////////////////////////////////
		// /// Parsing of Statements
		Set<? extends Element> setCmds = env.getElementsAnnotatedWith(symbol.class);
		root.appendChild(this.processDocXMLStatements(setCmds, doc));

		// ////////////////////////////////////////////////
		// /// Parsing of Types 
		Set<? extends Element> setTypes = env.getElementsAnnotatedWith(type.class);
		ArrayList<org.w3c.dom.Element> listEltOperatorsFromTypes = this.processDocXMLOperatorsFromTypes(setTypes, doc);
		
		org.w3c.dom.Element eltOperators = (org.w3c.dom.Element) root.getElementsByTagName(XMLElements.OPERATORS).item(0);
		for(org.w3c.dom.Element eltOp : listEltOperatorsFromTypes){
			eltOperators.appendChild(eltOp);
		}
		
		root.getElementsByTagName(XMLElements.OPERATORS_CATEGORIES).item(0).appendChild(new Category(doc, tc.getProperCategory("Types")).getElementDOM());
		
		
		// ////////////////////////////////////////////////
		// /// Parsing of Files 
		Set<? extends Element> setFiles = env.getElementsAnnotatedWith(file.class);
		ArrayList<org.w3c.dom.Element> listEltOperatorsFromFiles = this.processDocXMLOperatorsFromFiles(setFiles, doc);
		
		for(org.w3c.dom.Element eltOp : listEltOperatorsFromFiles){
			eltOperators.appendChild(eltOp);
		}	

		
		// //////////////////////
		// Final step:
		doc.appendChild(root);

		// ////////////////////////////////////////////////

		try {
			// Creation of the DOM source
			DOMSource source = new DOMSource(doc);

			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");

			transformer.transform(source, result);
			String stringResult = writer.toString();

			final PrintWriter docWriterXML = new PrintWriter(out);
			docWriterXML.append(stringResult).println("");
			docWriterXML.close();

		} catch (Exception e) {
			throw new NullPointerException("Erreur dans le Processor ");
		}
	}

	private ArrayList<org.w3c.dom.Element> processDocXMLOperatorsFromTypes(
			final Set<? extends Element> set, final Document doc) {
		
		ArrayList<org.w3c.dom.Element> eltOpFromTypes = new ArrayList<org.w3c.dom.Element>();
		for ( Element e : set ) {
			// Operators to be created:
			// - name_type: converts the parameter into the type name_type
			Operator op = new Operator(doc, tc.getProperCategory("Types"), e.getAnnotation(type.class).name());
			op.setOperands(((TypeElement) e).getQualifiedName().toString(), "", e.getAnnotation(type.class).name(), "");
			op.addOperand(new Operand(doc,"val",0,"any"));
			op.setDocumentation("Casts the operand into the type "+ e.getAnnotation(type.class).name());
			
			eltOpFromTypes.add(op.getElementDOM());
		}
		
		return eltOpFromTypes;
	}

	private ArrayList<org.w3c.dom.Element> processDocXMLOperatorsFromFiles(
			final Set<? extends Element> set, final Document doc) {
		
		ArrayList<org.w3c.dom.Element> eltOpFromTypes = new ArrayList<org.w3c.dom.Element>();
		for ( Element e : set ) {
			// Operators to be created:
			// - "is_"+name : test whether the operand parameter is of the given kind of file			
			// - name+"_file": converts the parameter into the type name_type
			Operator op_is = new Operator(doc, tc.getProperCategory("Files"), "is_" + e.getAnnotation(file.class).name(),
					"Tests whether the operand is a "+ e.getAnnotation(file.class).name() + " file.");
			op_is.setOperands(((TypeElement) e).getQualifiedName().toString(), "", "bool", "");
			op_is.addOperand(new Operand(doc,"val",0,"any"));
			// op_is.setDocumentation("Tests whether the operand is a "+ e.getAnnotation(file.class).name() + " file.");

			Operator op_file = new Operator(doc, tc.getProperCategory("Files"), e.getAnnotation(file.class).name() + "_file");
			op_file.setOperands(((TypeElement) e).getQualifiedName().toString(), "", "file", "");
			op_file.addOperand(new Operand(doc, "val", 0, "string"));
			
			String[] tabExtension = e.getAnnotation(file.class).extensions();
			String listExtension = "";
			if(tabExtension.length > 0){
				listExtension = tabExtension[0];
				if(tabExtension.length > 1){
					for(int i = 1; i< tabExtension.length; i++){
						listExtension = listExtension + ", " + tabExtension[i];
					}
				}
			}
			op_file.setDocumentation("Constructs a file of type "+ e.getAnnotation(file.class).name() + ". Allowed extensions are limited to " + listExtension);
			
			eltOpFromTypes.add(op_is.getElementDOM());
			eltOpFromTypes.add(op_file.getElementDOM());
		}
			
		return eltOpFromTypes;
	}
	
	private org.w3c.dom.Element processDocXMLOperatorsCategories(
											final Set<? extends ExecutableElement> set, final Document doc) {
		org.w3c.dom.Element operatorsCategories = doc.createElement(XMLElements.OPERATORS_CATEGORIES);
		for ( ExecutableElement e : set ) {
			String[] categoryNames = new String[1];
			// String categoryName;
			if(e.getAnnotation(operator.class) != null && e.getAnnotation(operator.class).category().length > 0) {
				categoryNames =e.getAnnotation(operator.class).category();
			} else {
				categoryNames[0] = tc.getProperCategory(e.getEnclosingElement().getSimpleName().toString());
			}
			
			NodeList nL = operatorsCategories.getElementsByTagName(XMLElements.CATEGORY);
			
			for(String categoryName : categoryNames) {
				int i = 0;
				boolean found = false;
				while (!found && i < nL.getLength()) {
					org.w3c.dom.Element elt = (org.w3c.dom.Element) nL.item(i);
					if ( categoryName.equals(tc.getProperCategory(elt.getAttribute(XMLElements.ATT_CAT_ID))) ) {
						found = true;
					}
					i++;
				}
	
				if ( !found ) {
					org.w3c.dom.Element category;
					category = doc.createElement(XMLElements.CATEGORY);
					category.setAttribute(XMLElements.ATT_CAT_ID, categoryName);
					operatorsCategories.appendChild(category);
				}
			}
		}
		return operatorsCategories;
	}

	private org.w3c.dom.Element processDocXMLOperators(final Set<? extends ExecutableElement> set,
		final Document doc) {
		org.w3c.dom.Element operators = doc.createElement(XMLElements.OPERATORS);

		for ( ExecutableElement e : set ) {
			nbrOperators++;
			List<? extends VariableElement> args = e.getParameters();
			Set<Modifier> m = e.getModifiers();
			boolean isStatic = m.contains(Modifier.STATIC);
			int arity = 0;
			// boolean firstInstanceOperator = false;

			if ( e.getAnnotation(doc.class) != null &&
				!"".equals(e.getAnnotation(doc.class).deprecated()) ) {
				// We just omit it
				// String strDeprecated = e.getAnnotation(doc.class).deprecated();
				// mes.printMessage(Kind.ERROR, "The deprecative message __" + strDeprecated );
			} else {
				// Look for an already parsed operator with the same name
				org.w3c.dom.Element operator =
						DocProcessorAnnotations.getOperatorElement(operators, e.getAnnotation(operator.class).value()[0]);
				if ( operator == null ) {
					operator = doc.createElement(XMLElements.OPERATOR);
					operator.setAttribute(XMLElements.ATT_OP_ID,
						tc.getProperOperatorName(e.getAnnotation(operator.class).value()[0]));
					operator.setAttribute(XMLElements.ATT_OP_NAME,
						tc.getProperOperatorName(e.getAnnotation(operator.class).value()[0]));
					
					operator.setAttribute(XMLElements.ATT_ALPHABET_ORDER, getAlphabetOrder(e.getAnnotation(operator.class).value()[0]));
				} 
				// Parse the alternative names of the operator
				// we will create one operator markup per alternative name
				for ( String name : e.getAnnotation(operator.class).value() ) {
					if ( !"".equals(name) &&
						!name.equals(e.getAnnotation(operator.class).value()[0]) ) {
						// Look for an already parsed operator with the same name
						org.w3c.dom.Element altElt = DocProcessorAnnotations.getOperatorElement(operators, name);
						if ( altElt == null ) {
							altElt = doc.createElement(XMLElements.OPERATOR);
							altElt.setAttribute(XMLElements.ATT_OP_ID, name);
							altElt.setAttribute(XMLElements.ATT_OP_NAME, name);
							altElt.setAttribute(XMLElements.ATT_OP_ALT_NAME, e.getAnnotation(operator.class).value()[0]);
							altElt.setAttribute(XMLElements.ATT_ALPHABET_ORDER, getAlphabetOrder(name));
							
							altElt.appendChild(getOperatorCategory(e,doc));
							operators.appendChild(altElt);
						} else {
							// Show an error in the case where two alternative names do not refer to
							// the same operator
							if ( !e.getAnnotation(operator.class).value()[0].equals(altElt
								.getAttribute("alternativeNameOf")) ) {
								mes.printMessage(Kind.ERROR,
									"The alternative name __" + name +
										"__ is used for two different operators: " +
										e.getAnnotation(operator.class).value()[0] + " and " +
										altElt.getAttribute("alternativeNameOf"));
							}
						}
					}
				}

				// Parse of categories
				
				// Category
				org.w3c.dom.Element categoriesElt;
				if ( operator.getElementsByTagName(XMLElements.OPERATORS_CATEGORIES).getLength() == 0 ) {
					categoriesElt = getOperatorCategory(e,doc,doc.createElement(XMLElements.OPERATORS_CATEGORIES));
				} else {
					categoriesElt = getOperatorCategory(e,doc,
						(org.w3c.dom.Element) operator.getElementsByTagName(XMLElements.OPERATORS_CATEGORIES).item(0));
				}
				operator.appendChild(categoriesElt);
				
				// operator.appendChild(getOperatorElement(e,doc,operator));
				
				// Parse the combinaison operands / result
				org.w3c.dom.Element combinaisonOpResElt;
				if ( operator.getElementsByTagName("combinaisonIO").getLength() == 0 ) {
					combinaisonOpResElt = doc.createElement("combinaisonIO");
				} else {
					combinaisonOpResElt =
						(org.w3c.dom.Element) operator.getElementsByTagName("combinaisonIO").item(0);
				}

				org.w3c.dom.Element operands = doc.createElement("operands");
				operands.setAttribute("returnType", tc.getProperType(e.getReturnType().toString()));
				operands.setAttribute("contentType", "" +
					e.getAnnotation(operator.class).content_type());
				operands.setAttribute("type", "" + e.getAnnotation(operator.class).type());

				// To specify where we can find the source code of the class defining the operator
				String pkgName = "" + ((TypeElement) e.getEnclosingElement()).getQualifiedName();
				// Now we have to deal with Spatial operators, that are defined in inner classes
				if ( pkgName.contains("Spatial") ) {
					// We do not take into account what is after 'Spatial'
					pkgName = pkgName.split("Spatial")[0] + "Spatial";
				}
				pkgName = pkgName.replace('.', '/');
				pkgName = pkgName + ".java";
				operands.setAttribute("class", pkgName);

				if ( !isStatic ) {
					org.w3c.dom.Element operand = doc.createElement((XMLElements.OPERAND));
					operand.setAttribute("type", tc.getProperType(e.getEnclosingElement().asType()
						.toString()));
					operand.setAttribute("position", "" + arity);
					arity++;
					operand.setAttribute("name", e.getEnclosingElement().asType().toString()
						.toLowerCase());
					operands.appendChild(operand);
				}
				if ( args.size() > 0 ) {
					int first_index = args.get(0).asType().toString().contains("IScope") ? 1 : 0;
					for ( int i = first_index; i <= args.size() - 1; i++ ) {
						org.w3c.dom.Element operand = doc.createElement((XMLElements.OPERAND));
						operand
							.setAttribute("type", tc.getProperType(args.get(i).asType().toString()));
						operand.setAttribute("position", "" + arity);
						arity++;
						operand.setAttribute("name", args.get(i).getSimpleName().toString());
						operands.appendChild(operand);
					}
				}
				// operator.setAttribute("arity", ""+arity);
				combinaisonOpResElt.appendChild(operands);
				operator.appendChild(combinaisonOpResElt);

				// /////////////////////////////////////////////////////
				// Parsing of the documentation
				org.w3c.dom.Element docElt;
				if(operator.getElementsByTagName("documentation").getLength() == 0){
					docElt = DocProcessorAnnotations.getDocElt(e.getAnnotation(doc.class), doc, mes, "Operator " + operator.getAttribute("name"), tc, e);
				} else {
					docElt = DocProcessorAnnotations.getDocElt(e.getAnnotation(doc.class), doc,
							(org.w3c.dom.Element) operator.getElementsByTagName("documentation").item(0),
							mes, "Operator " + operator.getAttribute("name"), tc, e);
				}
				
				if(docElt != null){
					operator.appendChild(docElt);
				}
				
				operators.appendChild(operator);
			}
		}
		return operators;
	}

	private org.w3c.dom.Element processDocXMLSkills(final Set<? extends Element> setSkills,
		final Document doc) {

		org.w3c.dom.Element skills = doc.createElement("skills");

		for ( Element e : setSkills ) {
			nbrSkills++;
			org.w3c.dom.Element skillElt = doc.createElement("skill");

			skillElt.setAttribute("id", e.getAnnotation(skill.class).name());
			skillElt.setAttribute("name", e.getAnnotation(skill.class).name());

			// get extends
			skillElt.setAttribute("class", ((TypeElement) e).getQualifiedName().toString());
			skillElt.setAttribute("extends", ((TypeElement) e).getSuperclass().toString());
			
			org.w3c.dom.Element docEltSkill = 
					DocProcessorAnnotations.getDocElt(e.getAnnotation(doc.class), doc, mes, e.getSimpleName().toString(), tc, null);
			if(docEltSkill != null){
				skillElt.appendChild(docEltSkill);
			}

			// Parsing of vars
			if ( e.getAnnotation(vars.class) != null ) {
				org.w3c.dom.Element varsElt = doc.createElement("vars");
				for ( var v : e.getAnnotation(vars.class).value() ) {
					org.w3c.dom.Element varElt = doc.createElement("var");
					varElt.setAttribute("name", v.name());
					varElt.setAttribute("type", tc.getTypeString(Integer.valueOf(v.type())));
					varElt.setAttribute("constant", "" + v.constant());
					
					org.w3c.dom.Element docEltVar = 
							DocProcessorAnnotations.getDocElt(v.doc(), doc, mes, "Var " + v.name() + " from " + skillElt.getAttribute("name"), tc, null);
					if(docEltVar != null){
						varElt.appendChild(docEltVar);
					}
					
					String dependsOn = new String();
					for ( String dependElement : v.depends_on() ) {
						dependsOn = ("".equals(dependsOn) ? "" : dependsOn + ",") + dependElement;
					}
					varElt.setAttribute("depends_on", dependsOn);
					varsElt.appendChild(varElt);
				}
				skillElt.appendChild(varsElt);
			}

			// Parsing of actions
			org.w3c.dom.Element actionsElt = doc.createElement("actions");

			for ( Element eltMethod : e.getEnclosedElements() ) {
				org.w3c.dom.Element actionElt = 
						DocProcessorAnnotations.getActionElt(eltMethod.getAnnotation(action.class), doc, mes, eltMethod, tc);		
				
				if(actionElt != null){
					actionsElt.appendChild(actionElt);
				}
			}
			skillElt.appendChild(actionsElt);

			skills.appendChild(skillElt);

			// Skills now have only one name

			// // Addition of other skills for alternative names of the species
			// for ( int i = 1; i < e.getAnnotation(skill.class).name().length; i++ ) {
			// org.w3c.dom.Element skillAlt = doc.createElement("skill");
			// skillAlt.setAttribute("id", e.getAnnotation(skill.class).name()[i]);
			// skillAlt.setAttribute("name", e.getAnnotation(skill.class).name()[i]);
			// skillAlt.setAttribute("alternativeNameOfSkill", id);
			// skills.appendChild(skillAlt);
			// }
		}
		// check the inheritance between Skills
		NodeList nlSkill = skills.getElementsByTagName("skill");
		for ( int i = 0; i < nlSkill.getLength(); i++ ) {
			org.w3c.dom.Element elt = (org.w3c.dom.Element) nlSkill.item(i);
			if ( elt.hasAttribute("extends") ) {
				if ( BASIC_SKILL.equals(elt.getAttribute("extends")) ) {
					elt.setAttribute("extends", "");
				} else {
					for ( int j = 0; j < nlSkill.getLength(); j++ ) {
						org.w3c.dom.Element testedElt = (org.w3c.dom.Element) nlSkill.item(j);
						if ( testedElt.getAttribute("class").equals(elt.getAttribute("extends")) ) {
							elt.setAttribute("extends", testedElt.getAttribute("name"));
						}
					}
				}
			}
		}

		return skills;
	}

	private org.w3c.dom.Element processDocXMLSpecies(final Set<? extends Element> setSpecies,
		final Document doc) {
		org.w3c.dom.Element species = doc.createElement("speciess");

		for ( Element e : setSpecies ) {
			org.w3c.dom.Element spec = doc.createElement("species");
			spec.setAttribute("id", e.getAnnotation(species.class).name());
			spec.setAttribute("name", e.getAnnotation(species.class).name());
			
			org.w3c.dom.Element docEltSkill = 
					DocProcessorAnnotations.getDocElt(e.getAnnotation(doc.class), doc, mes, e.getSimpleName().toString(), tc, null);
			if(docEltSkill != null){
				spec.appendChild(docEltSkill);
			}

			// Parsing of actions
			org.w3c.dom.Element actionsElt = doc.createElement("actions");
			for ( Element eltMethod : e.getEnclosedElements() ) {
				org.w3c.dom.Element actionElt = 
						DocProcessorAnnotations.getActionElt(eltMethod.getAnnotation(action.class), doc, mes, eltMethod, tc);		
				
				if(actionElt != null){
					actionsElt.appendChild(actionElt);
				}
			}
			spec.appendChild(actionsElt);
			
			species.appendChild(spec);
		}
		return species;
	}

	private org.w3c.dom.Element processDocXMLStatements(final Set<? extends Element> setStatement,
		final Document doc) {
		org.w3c.dom.Element statementsElt = doc.createElement("statements");

		for ( Element e : setStatement ) {
			nbrSymbols++;
			org.w3c.dom.Element statElt = doc.createElement("statement");
			if ( e.getAnnotation(symbol.class).name().length != 0 ) {
				statElt.setAttribute("id", e.getAnnotation(symbol.class).name()[0]);
				statElt.setAttribute("name", e.getAnnotation(symbol.class).name()[0]);
			} else {
				// TODO : case of variables declarations ... Variable, ContainerVariable,
				// NumberVariable
			}
			statElt.setAttribute("kind", "" + e.getAnnotation(symbol.class).kind());

			// Parsing of facets
			org.w3c.dom.Element facetsElt = 
				DocProcessorAnnotations.getFacetsElt(e.getAnnotation(facets.class), doc, mes, statElt.getAttribute("name"), tc);
			if(facetsElt != null){
				statElt.appendChild(facetsElt);
			}

			// Parsing of documentation
			org.w3c.dom.Element docstatElt = 
				DocProcessorAnnotations.getDocElt(e.getAnnotation(doc.class), doc, mes, "Statement " + statElt.getAttribute("name"), tc, null);
			if(docstatElt != null){
				statElt.appendChild(docstatElt);
			}

			// Parsing of inside
			org.w3c.dom.Element insideElt = 
				DocProcessorAnnotations.getInsideElt(e.getAnnotation(inside.class), doc);
			if(insideElt != null){
				statElt.appendChild(insideElt);
			}
			
			statementsElt.appendChild(statElt);
		}
		return statementsElt;
	}

	
	private org.w3c.dom.Element getOperatorCategory(final ExecutableElement e, final Document doc, org.w3c.dom.Element categoriesElt){	
		ArrayList<String> categories = new ArrayList<String>();
		NodeList nL = categoriesElt.getElementsByTagName(XMLElements.CATEGORY);
		for(int i = 0; i < nL.getLength() ; i++){
			categories.add(((org.w3c.dom.Element) nL.item(i)).getAttribute(XMLElements.ATT_CAT_ID));
		}
		
		if(e.getAnnotation(operator.class) != null && e.getAnnotation(operator.class).category().length > 0) {
			for(String categoryName : e.getAnnotation(operator.class).category()){
				if(!categories.contains(categoryName)){
					categories.add(categoryName);
					
					org.w3c.dom.Element catElt = doc.createElement(XMLElements.CATEGORY);
					catElt.setAttribute(XMLElements.ATT_CAT_ID, categoryName);
					categoriesElt.appendChild(catElt);
				}
			}
		} else {
			if(!categories.contains(tc.getProperCategory(e.getEnclosingElement().getSimpleName().toString()))) {
				org.w3c.dom.Element catElt = doc.createElement(XMLElements.CATEGORY);
				catElt.setAttribute(XMLElements.ATT_CAT_ID, tc.getProperCategory(e.getEnclosingElement().getSimpleName().toString()));
				categoriesElt.appendChild(catElt);
			}
		}		
		return categoriesElt;
	}
	
	private org.w3c.dom.Element getOperatorCategory(final ExecutableElement e, final Document doc){		
		org.w3c.dom.Element categoriesElt = doc.createElement(XMLElements.OPERATORS_CATEGORIES);
		
		return getOperatorCategory(e,doc,categoriesElt);
	}
	
	public static String getAlphabetOrder(String name) {
		String order = "";
		String lastChar = "z";
		
		for(int i = 0; i < cuttingLettersOperatorDoc.length ; i++){
			Character previousChar = (i==0) ? 'a' : cuttingLettersOperatorDoc[i-1];
			Character c = cuttingLettersOperatorDoc[i];
			
			if((i==0 && name.compareTo(c.toString()) < 0) 
					|| (name.compareTo(previousChar.toString()) >= 0 && name.compareTo(c.toString()) < 0)){   // name is < to cutting letter
				order = previousChar.toString() + ((Character)(Character.toChars(c-1))[0]).toString();
			} 
		}
		if ("".equals(order)) order = cuttingLettersOperatorDoc[cuttingLettersOperatorDoc.length-1].toString() + lastChar;
		
		return order;
	}
	
}