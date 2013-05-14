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
package msi.gaml.statements;

import java.util.List;
import msi.gama.common.interfaces.IKeyword;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.*;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.IContainer;
import msi.gaml.compilation.ISymbol;
import msi.gaml.descriptions.IDescription;
import msi.gaml.expressions.IExpression;
import msi.gaml.types.IType;

// A group of commands that can be executed on remote agents.

@symbol(name = IKeyword.ASK, kind = ISymbolKind.SEQUENCE_STATEMENT, with_sequence = true, remote_context = true)
@facets(value = { @facet(name = IKeyword.TARGET, type = { IType.CONTAINER, IType.AGENT }, optional = false),
	@facet(name = IKeyword.AS, type = { IType.SPECIES }, optional = true) }, omissible = IKeyword.TARGET)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT }, symbols = IKeyword.CHART)
@doc(value = "Allows an agent, the sender agent (that can be the [Sections151#global world agent]), to ask another (or other) agent(s) to perform a set of statements. " +
		"It obeys the following syntax, where the target attribute denotes the receiver agent(s):", 
	examples = {
		"ask receiver_agent(s) {",
		"     [statements]",
		"}"})
public class AskStatement extends AbstractStatementSequence {

	private AbstractStatementSequence sequence = null;
	private final IExpression target;

	public AskStatement(final IDescription desc) {
		super(desc);
		target = getFacet(IKeyword.TARGET);
		if ( target != null ) {
			setName("ask " + target.toGaml());
		}
	}

	@Override
	public void setChildren(final List<? extends ISymbol> com) {
		sequence = new AbstractStatementSequence(description);
		sequence.setName("commands of " + getName());
		sequence.setChildren(com);
	}

	@Override
	public Object privateExecuteIn(final IScope scope) throws GamaRuntimeException {
		Object t = target.value(scope);
		if ( t == null ) {
			scope.setStatus(ExecutionStatus.failure);
			return null;
		}
		IAgent[] targets;
		if ( t instanceof IContainer ) {
			targets = new IAgent[((IContainer) t).length(scope)];
			int index = 0;
			for ( Object o : (IContainer) t ) {
				if ( o instanceof IAgent ) {
					targets[index++] = (IAgent) o;
				} else {
					throw GamaRuntimeException.error("ask can only be invoked on agents. " + o + " is not an agent");
				}
			}
		} else if ( t instanceof IAgent ) {
			targets = new IAgent[1];
			targets[0] = (IAgent) t;
		} else {
			throw GamaRuntimeException.error("ask can only be invoked on agents. " + t + " is not an agent");
		}

		IAgent scopeAgent = scope.getAgentScope();
		scope.addVarWithValue(IKeyword.MYSELF, scopeAgent);
		for ( int i = 0, n = targets.length; i < n; i++ ) {
			IAgent remoteAgent = targets[i];
			if ( !remoteAgent.dead() && !scope.interrupted() ) {
				scope.execute(sequence, remoteAgent);
			}
			scope.setStatus(ExecutionStatus.skipped);
		}
		return null;
	}

}