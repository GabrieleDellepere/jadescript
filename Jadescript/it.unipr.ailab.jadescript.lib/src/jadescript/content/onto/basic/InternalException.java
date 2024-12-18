package jadescript.content.onto.basic;

import jade.content.onto.Ontology;
import jadescript.content.JadescriptPredicate;

public class InternalException implements JadescriptPredicate {
    private String description;

    private transient Throwable cause;

    public InternalException() {
        description = "";
    }


    public InternalException(String description) {
        this.description = description;
    }

    public InternalException(String description, Throwable cause){
        this.description = description;
        this.cause = cause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Throwable cause(){
        return cause;
    }

    @Override
    public Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }

    @SuppressWarnings("SameReturnValue")
    public jadescript.content.onto.Ontology __metadata_jadescript_content_onto_basic_InternalException() {
        return null;
    }

    @Override
    public String toString() {
        return "InternalException(description=\"" + description + "\")";
    }
}
