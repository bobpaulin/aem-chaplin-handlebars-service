package org.apache.sling.chaplin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.webresource.WebResourceInventoryManager;
import org.apache.sling.webresource.postprocessors.PostCompileProcess;
import org.apache.sling.webresource.util.JCRUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = true, label="Handlebars Chaplin Post Compile", immediate=true)
@Service
public class HandlebarsPostCompileWrapper implements PostCompileProcess {
	
	@org.apache.felix.scr.annotations.Property
	public static final String HANDLEBARS_TEMPLATE_SOURCE_PATH_FILTER = "handlebars.template.source.path.filter";
	
	@Reference
	private EventAdmin eventAdmin;
	
	private String handlebarsTemplateSourcePathFilter;
	
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public void activate(ComponentContext context)
    {
    	Dictionary config = context.getProperties();
    	handlebarsTemplateSourcePathFilter = PropertiesUtil.toString(config.get(HANDLEBARS_TEMPLATE_SOURCE_PATH_FILTER), "/apps/chaplin/handlebars-templates");
    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
		Event event = new Event(
				WebResourceInventoryManager.COMPILE_ALL_EVENT, properties);
		this.eventAdmin.postEvent(event);
    }
    
    public InputStream processCompiledStream(InputStream compiledSource) {
        InputStream start = new ByteArrayInputStream("define(['handlebars'], function(Handlebars) { return ".getBytes());
        
        InputStream end = new ByteArrayInputStream("});".getBytes());
        
        InputStream result = new SequenceInputStream(start, compiledSource);
        result = new SequenceInputStream(result, end);
        return result;
    }
    
    public boolean shouldProcess(Node sourceNode) {
        String extension = null;
        String mimeType = null;
        try{
        	if(!sourceNode.getPath().startsWith(handlebarsTemplateSourcePathFilter)){
        		return false;
        	}
        	
        	
            if (sourceNode.hasNode(Property.JCR_CONTENT)) {
                Node sourceContent = sourceNode.getNode(Property.JCR_CONTENT);
                if(sourceContent.hasProperty(Property.JCR_MIMETYPE))
                {
                    mimeType = sourceContent.getProperty(Property.JCR_MIMETYPE).getString();
                }
            }
           extension = JCRUtils.getNodeExtension(sourceNode);

        }catch(RepositoryException e)
        {
            //Log Exception
            log.info("Node Name can not be read.  Skipping node.");
        }
        
        return "hbs".equals(extension) || "handlebars".equals(extension) || "text/x-handlebars-template".equals(mimeType);
    }
}
