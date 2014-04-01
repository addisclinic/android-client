package org.moca.procedure;

import org.moca.R;
import org.moca.media.AudioPlayer;
import org.moca.media.EducationResource;
import org.moca.util.MocaUtil;
import org.w3c.dom.Node;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A ProcedureElement is an item that can be placed on a page in a Sana 
 * procedure. Typically there will only be one ProcedureElement per page, but 
 * this style suggestion is not enforced, and users can make XML procedure 
 * definitions that contain several ProcedureElements per page. 
 * <p/>
 * A ProcedureElement, generally speaking, asks a question and may allow for an 
 * answer. For example, a RadioElement poses a question and allows a user to 
 * choose among response buttons.
 * 
 * ProcedureElements are defined in XML and dynamically created from the XML in 
 * Sana.
 * <p/>
 * <ul type="none">
 * <li><b>Clinical Use </b> Defined by subclasses.</li>
 * <li><b>Collects </b> Defined by subclasses.</li>
 * </ul>
 * The <em>Collects</em> documentation should declare the format of any returned
 * answer strings
 * 
 * @author Sana Development Team
 */
public abstract class ProcedureElement {
    public static String TAG = ProcedureElement.class.getSimpleName();
    
    /**
     * An enumeration of the valid ProcedureElement types.
     * 
     * @author Sana Development Team
     */
    public static enum ElementType {
    	/** Provides text display */
    	TEXT(""),
    	
    	/** Provides text capture. */
        ENTRY(""), 
        
        /** Provides exclusive option selector  as a dropdown box. */
        SELECT(""), 
        
        /** An entry element for displaying/entering a patient identifier */
        PATIENT_ID(""),
        
        /** A non-exclusive option selector as a list of checkboxes. */
        MULTI_SELECT(""), 
        
        /** An exclusive multi-option selector as a list of radio buttons */
        RADIO(""), 
        
        /** Provides capture of one or more images */
        PICTURE("image.jpg"),
        
        /** Provides capture of a single audio resource. */
        SOUND("sound.3gp"), 
        
        /** Provides attachment of a binary file for upload */
        BINARYFILE("binary.bin"), 
        
        /** A marker for invalid elements */
        INVALID(""), 
        
        /** Provides capture of GPS coordinates */
        GPS(""),
        
        /** Provides capture of a date */ 
        DATE(""),
        
        /** Provides a viewable resource for patient education. */
        EDUCATION_RESOURCE(""),
        
        /** Provides access to 3rd party tools for data capture where the data
         *  is returned directly. 
         */
        PLUGIN(""),
        
        /** Provides access to 3rd party tools for data capture where the data
         *  is not returned directly and must be manually entered by the user. 
         */
        ENTRY_PLUGIN("");
    	
        private String filename;
        private ElementType(String filename) {
        	this.filename = filename;
        }
        
        /**
         * Returns the default filename for a given ElementType
         * @return
         */
        public String getFilename() {
        	return filename;
        }
    }

    protected String id;
    protected String question;
    protected String answer;
    protected String concept;
    
    // Resource of a corresponding figure for this element.
    protected String figure;
    // Resource of a corresponding audio prompt for this element.
    protected String audioPrompt;
    // Whether a null answer is allowed
    private boolean bRequired = false;
    
    private Procedure procedure;
    private Context cachedContext;
    private View cachedView;
    private AudioPlayer mAudioPlayer;
    private String helpText;

    /**
     * Constructs the view of this ProcedureElement
     * 
     *  @param c the current Context
     */
    protected abstract View createView(Context c);
    
    void clearCachedView() {
    	cachedView = null;
    }
    
    /**
     * Constructs a new Instance.
     * 
     * @param id The unique identifier of this element within its procedure.
     * @param question The text that will be displayed to the user as a question
     * @param answer The result of data capture.
     * @param concept A required categorization of the type of data captured.
     * @param figure An optional figure to display to the user.
     * @param audioPrompt An optional audio prompt to play for the user. 
     */
    protected ProcedureElement(String id, String question, String answer, 
    		String concept, String figure, String audioPrompt) 
    {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.concept = concept;
        this.figure = figure;
        this.audioPrompt = audioPrompt;
    }
    
    /**
     * A reference to the enclosing Procedure
     * @return A Procedure instance.
     */
    protected Procedure getProcedure() { // set the ImageView bounds to match the Drawable's dimensions
        return procedure;
    }
    
    /**
     * Sets the enclosing procedure
     * @param procedure the new enclosing procedure.
     */
    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }
    
    /** 
     * Whether this element is considered active.
     * @return
     */
    protected boolean isViewActive() {
        return !(cachedView == null);
    }
    
    /**
     * A cached Context.
     * @return The Context this element holds a reference to.
     */
    protected Context getContext() {
        return cachedContext;
    }
    
    /**
     * A visible representation of this object.
     * @param c The Context which will be used in the View constructors for this 
     * 			object's representation.
     * @return A new view of this object or cached view if it exists.
     */
    public View toView(Context c) {
        if(cachedView == null || cachedContext != c) {
            cachedView = createView(c);
            cachedContext = c;
        }
        return cachedView;
    }

	/**
	 * Returns the ElementType of this element as defined in the 
	 * ProcedureElement ElementType enum.
	 */
    public abstract ElementType getType(); 
    
    /** 
     * Gets the value of the answer attribute.
     * 
     * @return A String representation of collected data.
     */
    public String getAnswer(){
    	 return answer;
    }
    
    /** 
     * Set the value of the answer attribute as a String representation of 
     * collected data.
     * 
     * @param the new answer 
     */
    public void setAnswer(String answer){
    	this.answer = answer;
    }
    
    /**
     * Whether this element is considered required
     * @return
     */
    public boolean isRequired() {
    	return bRequired;
    }
    
    /**
     * Sets the required state of this element.
     * @param required The new required state.
     */
    public void setRequired(boolean required) {
    	this.bRequired = required;
    }
    
    /**
     * Help text associated with this element
     * @return An informative string.
     */
    public String getHelpText() {
    	return helpText;
    }
    
    /**
     * Sets the help text for this instance.
     * @param helpText the new help string.
     */
    public void setHelpText(String helpText) {
    	this.helpText = helpText;
    }
    
    /**
     * Whether this element is valid.
     * @return true if not required or required and answer is not empty
     * @throws ValidationError
     */
    public boolean validate() throws ValidationError {
    	if (bRequired && "".equals(getAnswer().trim())) {
    		String msg = TextUtils.isEmpty(helpText)? 
    				getString(R.string.general_input_required): helpText;
    		throw new ValidationError(msg);
    	}
    	return true;
    }
    
    /**
     * Tell the element's widget to refresh itself. 
     */
    public void refreshWidget() {
    
    }
    
    /**
     * Writes a string representation of this object to a StringBuilder. 
     * Extending classes should override appendOptionalAttributes if they 
     * require attributes beyond those defined in this class.
     * 
     * @param sb the builder to write to.
     */
    public void buildXML(StringBuilder sb){
        sb.append("<Element ");
        sb.append("type=\"" + getType().name() + "\" ");
        sb.append("id=\"" + getId()+ "\" ");
        sb.append("question=\"" + getQuestion()+ "\" ");
        sb.append("answer=\"" + getAnswer()+ "\" ");
        sb.append("figure=\"" + getFigure()+ "\" ");
        sb.append("concept=\"" + getConcept()+ "\" ");
        sb.append("audio=\"" + getAudioPrompt()+ "\" ");
        sb.append("required=\"" + isRequired()+ "\" ");
        appendOptionalAttributes(sb);
        sb.append("/>\n");
    }

    protected void appendOptionalAttributes(StringBuilder sb){
    	return;
    }
    
    /**
	 * Build the XML representation of this ProcedureElement. Should only use
	 * this if you intend to use only the XML for this element. If you are
	 * building the XML for this Procedure, then prefer buildXML with a
	 * StringBuilder since String operations are slow.
	 */
    public String toXML() {
    	StringBuilder sb = new StringBuilder();
    	buildXML(sb);
    	return sb.toString();
    }
    
    /**
     * Create an element from an XML element node of a procedure definition.
     * @param node a Node object containing a ProcedureElement representation
     */
    public static ProcedureElement createElementfromXML(Node node) throws 
    	ProcedureParseException 
    {
        //Log.i(TAG, "fromXML(" + node.getNodeName() + ")");
        
        if(!node.getNodeName().equals("Element")) {
            throw new ProcedureParseException("Element got NodeName " 
            		+ node.getNodeName());
        }

        String questionStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"question", "");
        String answerStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"answer", null);
        String typeStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"type", "INVALID");
        String conceptStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"concept", "");
        String idStr = MocaUtil.getNodeAttributeOrFail(node, "id", 
        		new ProcedureParseException("Element doesn't have id number"));
        String figureStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"figure", "");
        String audioStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"audio", "");
        
        ElementType etype = ElementType.valueOf(typeStr);
        
        ProcedureElement el = null;
        switch(etype) {
        case TEXT:
            el = TextElement.fromXML(idStr, questionStr, answerStr, conceptStr, 
            		figureStr, audioStr, node);
            break;
        case ENTRY:
            el = TextEntryElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case SELECT:
            el = SelectElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case MULTI_SELECT:
            el = MultiSelectElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case RADIO:
            el = RadioElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case PICTURE:
            el = PictureElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case SOUND:
            el = SoundElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case GPS:
        	el = GpsElement.fromXML(idStr, questionStr, answerStr, 
        			conceptStr, figureStr, audioStr, node);
        	break;
        case BINARYFILE:
            el = BinaryUploadElement.fromXML(idStr, questionStr, answerStr, 
            		conceptStr, figureStr, audioStr, node);
            break;
        case PATIENT_ID:
        	el = PatientIdElement.fromXML(idStr, questionStr, answerStr, 
        			conceptStr, figureStr, audioStr, node);
        	break;
        case DATE:
        	el = DateElement.fromXML(idStr, questionStr, answerStr, conceptStr, 
        			figureStr, audioStr, node);
        	break;
        case EDUCATION_RESOURCE:
        	el = EducationResourceElement.fromXML(idStr, questionStr, answerStr,
        			conceptStr, figureStr, audioStr, node);
        	break;
        case PLUGIN:
        	el = PluginElement.fromXML(idStr, questionStr, answerStr, 
        			conceptStr, figureStr, audioStr, node);
        	break;
        case ENTRY_PLUGIN:
        	el = PluginEntryElement.fromXML(idStr, questionStr, answerStr, 
        			conceptStr, figureStr, audioStr, node);
        	break;
        case INVALID:
        default:
            throw new ProcedureParseException("Got invalid node type : " 
            		+ etype);
        }
        
        if (el == null) {
        	throw new ProcedureParseException("Failed to parse node with id " 
        			+ idStr);
        }
        
        String helpStr = MocaUtil.getNodeAttributeOrDefault(node, "helpText",
        		"");
        el.setHelpText(helpStr);
        
        String requiredStr = MocaUtil.getNodeAttributeOrDefault(node, 
        		"required", "false");
        if ("true".equals(requiredStr)) {
        	el.setRequired(true);
        } else if ("false".equals(requiredStr)) {
        	el.setRequired(false);
        } else {
        	throw new ProcedureParseException("Argument to \'required\' "+
        			"attribute invalid for id " + idStr 
        			+ ". Must be \'true\' or \'false\'");
        }
        return el;
    }
    
    /** @return The value of the id attribute */
    public String getId() {
        return id;
    }

    /** @return Gets the identifier for any associated education resources */
    public String mediaId(){
    	return EducationResource.toId(concept+question);
    }
    
    /**
     * @return the question string originally defined in the XML procedure 
     * definition.
     */
    public String getQuestion() {
        return question;
    }
    
    /**
     * @return the medical concept associated with this ProcedureElement
     */
    public String getConcept() {
    	return concept;
    }
    
    /**
     * @return the figure URL associated with this ProcedureElement
     */
    public String getFigure() {
    	return figure;
    }
    /** @return true if this instance has an audio prompt */
    boolean hasAudioPrompt() {
    	return !"".equals(audioPrompt);
    }
    
    /** plays this instance's audio prompt */
    void playAudioPrompt() {
    	if (mAudioPlayer != null)
    		mAudioPlayer.play();
    }
    
    /** @return the audioPrompt string */
    public String getAudioPrompt() {
    	return audioPrompt;
    }
    
    /**
     * Returns a localized String from the application package's default string
     * table.
     * 
     * @param resId Resource Id for the string
     * @return
     */
    public String getString(int resId){
    	return getContext().getString(resId);
    }
    
    /** 
     * Appends another View object a view of this object to a new View .
     * @param c A valid Context.
     * @param v The view to append first.
     * @return A new View containing the parameter View and a View of this 
     * 		object.
     */
    public View encapsulateQuestion(Context c, View v) {
    	TextView textView = new TextView(c);
    	textView.setText(this.question);
    	textView.setGravity(Gravity.CENTER_HORIZONTAL);
    	textView.setTextAppearance(c, android.R.style.TextAppearance_Large);
    	View questionView = textView;
    	
        ImageView imageView = null;
        
        //Set accompanying figure
        if(!TextUtils.isEmpty(figure)) {    
	        try{
	        	String imagePath = c.getPackageName() + ":" + figure;
	        	Log.d(TAG, "Using figure: " + figure);
	        	int resID = c.getResources().getIdentifier(figure, null, 
	        			null);
	        	Log.d(TAG, "Using figure id: " + resID);
	        	imageView = new ImageView(c);
	        	imageView.setImageResource(resID);
	        	imageView.setAdjustViewBounds(true);
	        	 // set the ImageView bounds to match the Drawable's dimensions
	        	imageView.setLayoutParams(new Gallery.LayoutParams(
	        			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        	imageView.setPadding(10,10,10,10);
	        }
	        catch(Exception e){
	        	Log.e(TAG, "Couldn't find resource figure " + e.toString());
	        }
        }
        
        if (hasAudioPrompt()) {
        	try {
        		String resourcePath = c.getPackageName() + ":" + audioPrompt;
        		int resID = c.getResources().getIdentifier(resourcePath, 
        				null, null);
        		Log.i(TAG, "Looking up ID for resource: " + resourcePath + ", " 
        				+ "got " + resID);
        		
        		if (resID != 0) {
	        		mAudioPlayer = new AudioPlayer(resID);
	        		View playerView = mAudioPlayer.createView(c);
	        		playerView.setLayoutParams(new LayoutParams(
	        			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        		LinearLayout audioPromptView = new LinearLayout(c);
	                audioPromptView.setOrientation(LinearLayout.HORIZONTAL);
	                audioPromptView.setLayoutParams(new LayoutParams(
	                	LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	                audioPromptView.setGravity(Gravity.CENTER);
	                
	                // Insert the play button to the left of the current 
	                // question view.
	                audioPromptView.addView(playerView);
	                audioPromptView.addView(questionView);
	                questionView = audioPromptView;
        		}
        	} catch (Exception e) {
        		Log.e(TAG, "Couldn't find resource for audio prompt: " 
        				+ e.toString());
        	}
        }
        Log.d(TAG, "Loaded: " +this.toString());
    	LinearLayout ll = new LinearLayout(c);
    	ll.setOrientation(LinearLayout.VERTICAL);

    	//Add to layout
    	ll.addView(questionView);
    	if (imageView != null)
    		ll.addView(imageView);
    	if(v != null){
    		LinearLayout viewHolder = new LinearLayout(c);
    		viewHolder.addView(v);
    		viewHolder.setGravity(Gravity.CENTER_HORIZONTAL);
    		ll.addView(viewHolder);
    	}
    	ll.setGravity(Gravity.CENTER);
    	ll.setPadding(10, 0, 10, 0);
    	ll.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
    			LayoutParams.WRAP_CONTENT));
        return ll;
    }
    
    @Override
    public String toString(){
    	return String.format("ProcedureElement: type=%s, concept=%s, "
    		+"required=%s, id=%s, question=%s, figure=%s, audio=%s, answer=%s",
    	    getType(), concept, bRequired, id, question, figure, audioPrompt, 
    	    answer);
    }
    
    /** 
     * Creates the element from an XML procedure definition.
     * 
     * @param id The unique identifier of this element within its procedure.
     * @param question The text that will be displayed to the user as a question
     * @param answer The result of data capture.
     * @param concept A required categorization of the type of data captured.
     * @param figure An optional figure to display to the user.
     * @param audio An optional audio prompt to play for the user. 
     * @param node The source xml node. 
     * @return A new element.
     * @throws ProcedureParseException if an error occurred while parsing 
     * 		additional information from the Node
     */
    public static ProcedureElement fromXML(String id, String question, 
    	String answer, String concept, String figure, String audio, Node node) 
    	throws ProcedureParseException 
    {
    	throw new UnsupportedOperationException();
    }
}
