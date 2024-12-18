/*
 * generated by Xtext 2.25.0
 */
package it.unipr.ailab.jadescript.ui.labeling;

import com.google.inject.Inject;

import it.unipr.ailab.jadescript.jadescript.Agent;
import it.unipr.ailab.jadescript.jadescript.Behaviour;
import it.unipr.ailab.jadescript.jadescript.Concept;
import it.unipr.ailab.jadescript.jadescript.GlobalFunctionOrProcedure;
import it.unipr.ailab.jadescript.jadescript.NamedElement;
import it.unipr.ailab.jadescript.jadescript.OnActivateHandler;
import it.unipr.ailab.jadescript.jadescript.OnBehaviourFailureHandler;
import it.unipr.ailab.jadescript.jadescript.OnCreateHandler;
import it.unipr.ailab.jadescript.jadescript.OnDeactivateHandler;
import it.unipr.ailab.jadescript.jadescript.OnDestroyHandler;
import it.unipr.ailab.jadescript.jadescript.OnExceptionHandler;
import it.unipr.ailab.jadescript.jadescript.OnExecuteHandler;
import it.unipr.ailab.jadescript.jadescript.OnMessageHandler;
import it.unipr.ailab.jadescript.jadescript.OnNativeEventHandler;
import it.unipr.ailab.jadescript.jadescript.Ontology;
import it.unipr.ailab.jadescript.jadescript.OntologyAction;
import it.unipr.ailab.jadescript.jadescript.Predicate;
import it.unipr.ailab.jadescript.jadescript.Proposition;

import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.xtext.xbase.ui.labeling.XbaseLabelProvider;

/**
 * Provides labels for EObjects.
 * 
 * See
 * https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#label-provider
 */
@SuppressWarnings("restriction")
public class JadescriptLabelProvider extends XbaseLabelProvider {

	@Inject
	public JadescriptLabelProvider(final AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}

	public String text(final OnMessageHandler a) {
		String performative = a.getPerformative();
		performative = (performative == null) ? "message" : performative;
		return "on " + performative;
	}

	public String image(final OnMessageHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnNativeEventHandler a) {
		return ("on native");
	}

	public String image(final OnNativeEventHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnCreateHandler a) {
		return ("on create");
	}

	public String image(final OnCreateHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnDestroyHandler a) {
		return ("on destroy");
	}

	public String image(final OnDestroyHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnActivateHandler a) {
		return ("on activate");
	}

	public String image(final OnActivateHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnDeactivateHandler a) {
		return "on deactivate";
	}

	public String image(final OnDeactivateHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnExecuteHandler a) {
		return "on execute";
	}

	public String image(final OnExecuteHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnExceptionHandler a) {
		return ("on exception");
	}

	public String image(final OnExceptionHandler e) {
		return "redo_edit.png";
	}

	public String text(final OnBehaviourFailureHandler a) {
		return ("on behaviour failure");
	}

	public String image(final OnBehaviourFailureHandler e) {
		return "redo_edit.png";
	}

	public String text(final Agent a) {
		return a.getName() + " - Agent";
	}

	public String image(final Agent a) {
		return "agent.png";
	}

	public String text(final Ontology o) {
		return o.getName() + " - Ontology";
	}

	public String image(final Ontology o) {
		return "ontology.png";
	}

	public String image(final Behaviour b) {
		return "behaviour.png";
	}

	public String text(final Behaviour b) {
		if (b instanceof Behaviour) {
			return ((Behaviour) b).getName() + " - " + ((Behaviour) b).getType() + " Behaviour";
		} else if (b instanceof NamedElement) {
			return ((NamedElement) b).getName() + " - Behaviour";
		}
		return "Behaviour";
	}

	public String text(final GlobalFunctionOrProcedure gfop) {
		return gfop.getName() + " - " + (gfop.isFunction() ? "Function" : "Procedure");
	}

	public String image(final GlobalFunctionOrProcedure gfop) {
		return "global_operation.png";
	}

	public String image(final Concept c) {
		return "concept.png";
	}

	public String image(final Predicate c) {
		return "pred.png";
	}

	public String image(final Proposition c) {
		return "pred.png";
	}

	public String image(final OntologyAction a) {
		return "action.png";
	}

	public String text(final Concept c) {
		return c.getName() + " - concept";
	}

	public String text(final Predicate c) {
		return c.getName() + " - predicate";
	}

	public String text(final Proposition c) {
		return c.getName() + " - proposition";
	}

	public String text(final OntologyAction c) {
		return c.getName() + " - action";
	}

}
