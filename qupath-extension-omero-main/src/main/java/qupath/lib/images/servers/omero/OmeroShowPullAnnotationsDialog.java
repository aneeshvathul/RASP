package qupath.lib.images.servers.omero;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.lang.Integer;
import java.lang.Boolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.IndexedCheckModel;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ListCell;
import javafx.util.Callback;
import javafx.stage.Stage;

import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.servers.omero.OmeroObjects.OmeroObjectType;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.gui.viewer.QuPathViewer;

/*
	This file creates a display of all OMERO annotations
	available to be pulled into QuPath. From this display (List
	View), the user is able to select the specific annotations
	they would like to pull into QuPath. Each pulling option is
	denoted by a uniquely identifiable description, inlcuding the
	annotation's name if provided.

*/


public final class OmeroShowPullAnnotationsDialog {

	
	static String title = "Show annotation pulling options";


	static String pathObjectDisplayName(PathObject ann){
		ROI roi = ann.getROI();
		String displayName = "";

		String annName = (ann.getName() != null) ? ann.getName() : "Annotation";
		
		displayName += "("+annName+") ";
		displayName += "("+roi.getRoiName()+") ";
		
		String pathClassRep = (ann.getPathClass() != null) ? String.valueOf(ann.getPathClass())
			: "Indeterminate Path Class";

		displayName += "("+pathClassRep+") ";

		String centroidX = String.format("%.2f", roi.getCentroidX());
		String centroidY = String.format("%.2f", roi.getCentroidY());

		displayName += "COORDS: "+"("+centroidX+", "+centroidY+")";

		return displayName;
	}

	/* 
		Displays and gets all the annotations the user has selected
		to pull from OMERO. (Though the ListView selection process
		in QuPath)
	*/
	
	static Collection<PathObjectWithMetadata> getPullSelections(QuPathGUI qupath , QuPathViewer viewer, OmeroWebImageServer omeroServer) {

		HashMap<String, Collection<PathObjectWithMetadata>> usersToAnnotations = omeroServer.getUserToPmdMap(viewer, qupath);
		String[] users = usersToAnnotations.keySet().toArray(new String[0]);
		if (users.length == 1){
			Dialogs.showErrorMessage(title, "No annotations to pull!");
			return new ArrayList<>();
		}

		else {
			for (int i = 0; i<users.length; i++){
				if (users[i] == "All Users"){
					String temp = users[0];
					users[0] = users[i];
					users[i] = temp;
				}
			}

			GridPane grid = new GridPane();

			CheckBox allUsersCheckBox = new CheckBox(users[0]);
			allUsersCheckBox.setIndeterminate(false);
			grid.add(allUsersCheckBox, 1, 1);

			Collection<CheckBox> checkBoxes = new ArrayList<>();

			Dialog<String> dialog = new Dialog<>();
			dialog.setTitle("Select Annotations to Pull");
			dialog.setHeaderText("Select users and their associated annotations"+"\n"
				+"(Ctrl+LeftClick to select multiple annotations)");
			dialog.setResizable(true);

					
			for (int i = 1; i<users.length; i++){
				CheckBox cb = new CheckBox(users[i]);
				cb.setIndeterminate(false);
				grid.add(cb, 1, i+1);
				checkBoxes.add(cb);
			}
			dialog.getDialogPane().setContent(grid);

			ListView<PathObjectWithMetadata> selectableAnnsListView = new ListView<PathObjectWithMetadata>();
			Collection<PathObjectWithMetadata> selectableAnnsSet = new HashSet<>();
			selectableAnnsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			selectableAnnsListView.setPrefWidth(475);

			selectableAnnsListView.setCellFactory(new Callback<ListView<PathObjectWithMetadata>, ListCell<PathObjectWithMetadata>>() {
				@Override
				public ListCell<PathObjectWithMetadata> call(ListView<PathObjectWithMetadata> param) {
					 ListCell<PathObjectWithMetadata> cell = new ListCell<PathObjectWithMetadata>() {
						@Override
						protected void updateItem(PathObjectWithMetadata ann, boolean empty) {
							super.updateItem(ann, empty);
							if (ann != null) {
								setText(pathObjectDisplayName(ann.getPathObject()));
							} 
							else {
								setText(null);
							}
						}
					 };
					return cell;
				}
			});

			for (CheckBox cb : checkBoxes){
				cb.selectedProperty().addListener(
					(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
						selectableAnnsSet.clear();
					   	for (CheckBox cbl : checkBoxes){
							if (cbl.isSelected()){
								selectableAnnsSet.addAll(usersToAnnotations.get(cbl.getText()));
							}
						}
						ObservableList<PathObjectWithMetadata> selectionsToAdd = FXCollections.observableArrayList(selectableAnnsSet); 
						selectableAnnsListView.setItems(selectionsToAdd);

				});
			}

			allUsersCheckBox.selectedProperty().addListener(
					(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
						selectableAnnsSet.clear();
					   	if (allUsersCheckBox.isSelected()){
							for (CheckBox cb : checkBoxes){
								cb.setSelected(false);
								cb.setDisable(true);
							}
							selectableAnnsSet.addAll(usersToAnnotations.get(allUsersCheckBox.getText()));
						}
						else{
							for (CheckBox cb : checkBoxes){
								cb.setDisable(false);
							}
						}
						ObservableList<PathObjectWithMetadata> selectionsToAdd = FXCollections.observableArrayList(selectableAnnsSet); 
						selectableAnnsListView.setItems(selectionsToAdd);

			});


			grid.add(selectableAnnsListView, 1, users.length+1);
					
			ButtonType buttonTypeOk = new ButtonType("Pull", ButtonData.OK_DONE);
			dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
			
			dialog.setResultConverter(new Callback<ButtonType, String>() {
				@Override
				public String call(ButtonType b) {
			
					if (b == buttonTypeOk) {
						return "Confirmed";
					}
			
					return "Canceled";
				}
			});

			Optional<String> status = dialog.showAndWait();
			
			if (status.get() == "Confirmed"){

				if (selectableAnnsListView.getSelectionModel().getSelectedItems().size() == 0){
					boolean checkBoxesAreSelected = false;
					for (CheckBox cb : checkBoxes){
						if (cb.isSelected()){
							checkBoxesAreSelected = true;
							break;
						}
					}
					if (allUsersCheckBox.isSelected()){
						checkBoxesAreSelected = true;
					}
					
					if (!checkBoxesAreSelected){
						Dialogs.showErrorMessage(title, "No users or annotations selected! Please select annotations.");
						return new ArrayList<>();
					}

					var confirm = Dialogs.showConfirmDialog("Pull annotations", String.format("No annotations are selected. Pull all annotations"+
						" from selected users instead? (%d %s)", 
						selectableAnnsSet.size(),
						(selectableAnnsSet.size() == 1 ? "object" : "objects")));
					
					if (confirm){
						return selectableAnnsSet;
					}

				}

				Collection<PathObjectWithMetadata> finalSendList = selectableAnnsListView.getSelectionModel().getSelectedItems().stream().collect(Collectors.toList());

				return finalSendList;
			}

			return new ArrayList<>();
		}

		
	}
}








