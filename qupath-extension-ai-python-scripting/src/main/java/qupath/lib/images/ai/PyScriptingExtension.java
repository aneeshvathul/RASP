package qupath.lib.images.ai;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;
import qupath.lib.gui.tools.PaneTools;

/**
 * Extension to access images hosted on OMERO.
 */
public class PyScriptingExtension implements QuPathExtension {
	
	private final static Logger logger = LoggerFactory.getLogger(PyScriptingExtension.class);
	
	private static boolean alreadyInstalled = false;
	
	
	@Override
	public void installExtension(QuPathGUI qupath) {
		if (alreadyInstalled)
			return;
		
		logger.debug("Installing PyScripting extension");
		
		alreadyInstalled = true;
		var actionExportMasks = ActionTools.createAction(new BinaryMaskExport(qupath), "Export Annotations as Binary Masks");
		var actionImportMasks = ActionTools.createAction(new BinaryMaskImport(qupath), "Import Binary Masks as Annotations");
		var actionExecutePythonScript = ActionTools.createAction(new ExecutePythonScript(qupath), "Run Python Script");


		actionExportMasks.disabledProperty().bind(qupath.imageDataProperty().isNull());
		actionImportMasks.disabledProperty().bind(qupath.imageDataProperty().isNull());
		actionExecutePythonScript.disabledProperty().bind(qupath.imageDataProperty().isNull());

		
		MenuTools.addMenuItems(qupath.getMenu("Extensions", false), 
				MenuTools.createMenu("PyScripting",
						actionImportMasks,
						actionExportMasks,
						actionExecutePythonScript
    	            )
				);
	}
	

	@Override
	public String getName() {
		return "PyScripting extension";
	}

	@Override
	public String getDescription() {
		return "Adds support for running custom Python AI scripts on wholeslide images locally.";
	}

	
	@Override
	public Version getQuPathVersion() {
		return Version.parse("0.3.0-rc2");
	}

}
