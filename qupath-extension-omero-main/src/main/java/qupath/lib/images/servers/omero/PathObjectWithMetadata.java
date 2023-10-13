package qupath.lib.images.servers.omero;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;

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


/*
	Currently, the "ID" feature is not supported and
	is a placeholder. In the future, this feature may
	be supported to assist annotation versioning
	between QuPath and OMERO.

*/


public final class PathObjectWithMetadata {
	private PathObject pathObject;
	private String userName;
	private double id;

	public PathObjectWithMetadata(PathObject pathObject, String userName, double id){
		this.pathObject = pathObject;
		this.userName = userName;
		this.id = id;
	}

	// Returns the QuPath *PathObject* associated with a PathObjectWithMetadata instance
	public PathObject getPathObject(){
		return pathObject;
	}

	// Returns the username of a corresponding OMERO annotation
	public String getUserName(){
		return userName;
	}
	
	/*
	 *Note: not supported --> Returns the ID denoting a specific OMERO annotation
	and its respective QuPath annotation. This ID is irrespective of annotation version.
	*/
	public double getID(){
		return id;
	}

	// Reassgins ID to a PathObjectWithMetadata representation of an OMERO/QuPath annotation
	public void setID(double newId){
		this.id = newId;
	}

	// Returns a qualitative/quantitative string description of a PathObjectWithMetadata's ROI
	public String roiDescription(){
		ROI roi = pathObject.getROI();

		String desc = "";
		desc += "TYPE AND BOUNDING BOX: " + roi.toString() + ", ";
		desc += "AREA: " + String.valueOf(roi.getArea()) + ", ";
		desc += "LENGTH: " + String.valueOf(roi.getLength()) + ", ";
		desc += "CENTROID X: " + String.valueOf(roi.getCentroidX()) + ", ";
		desc += "CENTROID Y: " + String.valueOf(roi.getCentroidY()) + ", ";
		desc += "NUMBER OF POINTS: " + String.valueOf(roi.getNumPoints());
		desc += "PATH CLASS: " + String.valueOf(pathObject.getPathClass());

		return desc;
	}
}








