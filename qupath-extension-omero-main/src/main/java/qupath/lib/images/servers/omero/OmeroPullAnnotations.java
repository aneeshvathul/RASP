package qupath.lib.images.servers.omero;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Integer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.servers.omero.OmeroObjects.OmeroObjectType;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.gui.viewer.QuPathViewer;


public final class OmeroPullAnnotations implements Runnable {
	
	private QuPathGUI qupath;
	
	private final String title = "Pull objects from OMERO";


	OmeroPullAnnotations(QuPathGUI qupath) {
		this.qupath = qupath;
	}



	@Override
	public void run() {

		var viewer = qupath.getViewer();
		var server = viewer.getServer();

		if (!(server instanceof OmeroWebImageServer)) {
			Dialogs.showErrorMessage(title, "The current image is not from OMERO!");
			return;
		}

		var omeroServer = (OmeroWebImageServer) server;

		// Retrieve filtered annotations with user data
		Collection<PathObjectWithMetadata> finalSendWithMetadata = OmeroShowPullAnnotationsDialog.getPullSelections(qupath, viewer, omeroServer);

		Collection<PathObject> finalSend = new ArrayList<>();
		for (PathObjectWithMetadata pmd : finalSendWithMetadata){
			finalSend.add(pmd.getPathObject());
		}
	
		// If there are no annotations in OMERO, tell the user
		if (finalSend.size() == 0){
			return;
		}

		URI uri = server.getURIs().iterator().next();
		String objectString = "object" + (finalSend.size()>1 ? "s" : "");

		// Confirm that user wants to pull annotations from OMERO
		GridPane pane = new GridPane();
		PaneTools.addGridRow(pane, 0, 0, null, new Label(String.format("%d %s will be pulled from:", finalSend.size(), objectString)));
		PaneTools.addGridRow(pane, 1, 0, null, new Label(uri.toString()));
		var confirm = Dialogs.showConfirmDialog("Pull " + objectString + " from OMERO?", pane);

		if (confirm){
			// Pull the new annotations from OMERO and onto QuPath
			viewer.getHierarchy().addPathObjects(finalSend);
			Dialogs.showInfoNotification(StringUtils.capitalize(objectString) + " pulled successfully", String.format("%d %s %s successfully pulled from OMERO server", 
			finalSend.size(), 
					objectString, 
					(finalSend.size() == 1 ? "was" : "were")));
		}
	
		
	}
}








