package qupath.lib.images.ai;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.file.Path;
import java.lang.Integer;
import java.lang.Double;
import java.lang.Exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.projects.Project;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.classes.PathClassFactory;
import qupath.lib.regions.RegionRequest;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.RoiTools;

import qupath.imagej.tools.IJTools;

import javax.imageio.ImageIO;
import java.awt.Color;

import ij.measure.Calibration;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.gui.Roi;

public final class BinaryMaskImport implements Runnable {

    private QuPathGUI qupath;
	
	private final String title = "Import Annotations as Binary Masks";

    BinaryMaskImport(QuPathGUI qupath) {
		this.qupath = qupath;
	}
    

    private static PathObject writePathObjectFromMask(File file){
        try {
            // Read the image
            BufferedImage img = ImageIO.read(file);

            // Split the file name into parts: [Image name, Classification, Region]
            String[] components = file.getName().replace("-mask.png", "").split("_");
            int size = components.length;

            String classificationString = components[size-2];
            String regionString = components[size-1].replace("(", "").replace(")", "");

            // Create Path Class
            PathClass pathClass = null;
            if (!classificationString.equals("None")){
                pathClass = PathClassFactory.getPathClass(classificationString);
            }

            // Parse Coordinates
            String[] regionParts = regionString.split(",");
            double downsample = Double.parseDouble(regionParts[0]);
            int x = Integer.parseInt(regionParts[1]);
            int y = Integer.parseInt(regionParts[2]);

            // To create the ROI, travel into ImageJ
            ByteProcessor bp = new ByteProcessor(img);
            bp.setThreshold(127.5, Double.MAX_VALUE, ImageProcessor.NO_LUT_UPDATE);
            Roi roiIJ = new ThresholdToSelection().convert(bp);

            // Convert ImageJ ROI to a QuPath ROI
            Calibration cal = new Calibration();
            cal.xOrigin = -x/downsample;
            cal.yOrigin = -y/downsample;
            ROI roi = IJTools.convertToROI(roiIJ, cal, downsample, ImagePlane.getDefaultPlane());

            // Create & return the object
            PathObject pathObj = PathObjects.createAnnotationObject(roi);

            if (pathClass != null){
                pathObj.setPathClass(pathClass);
            }

            return pathObj;

        }
        
        catch (IOException ex){
            Dialogs.showErrorNotification("Could not write binary mask as annotations | File name: " + file.getName(), ex.getLocalizedMessage());
            return null; 
        }
    }

    @Override
    public void run(){
        QuPathViewer viewer = qupath.getViewer();
        var hierarchy = viewer.getHierarchy();
        var imageData = viewer.getImageData();
		ImageServer<BufferedImage> server = viewer.getServer();

        Project<BufferedImage> project = qupath.getProject();
        String imageName = project.getEntry(imageData).getImageName();

        // Directory Selection
        GridPane maskFolderField = new GridPane();
		maskFolderField.setVgap(5.0);

		TextField tf = new TextField();
		tf.setPrefWidth(400);

		PaneTools.addGridRow(maskFolderField, 0, 0, "Enter folder path", new Label("Enter the relative folder path (inside your project folder)"+
        " that you would like your binary masks to be imported from."));

		PaneTools.addGridRow(maskFolderField, 2, 0, "Enter folder path", tf, tf);

		boolean confirmDirectory = Dialogs.showConfirmDialog("Enter folder path", maskFolderField);
		if (!confirmDirectory)
		    return;

        Path projectPath = project.getPath();
        String pathInput = projectPath.getParent().toString() + "\\" + tf.getText();

        File inputDirectory = new File(pathInput);
        if (!inputDirectory.isDirectory()) {
            Dialogs.showErrorMessage(title, "Path specified does not exist!");
            return;
        }

        // Only parse files that contain the specified text; set to '' if all files should be included
        // (This is used to avoid adding masks intended for a different image)
        HashSet<File> files = new HashSet(Arrays.asList(inputDirectory.listFiles()));
        for (File f : files){
            if (!(f.isFile() && f.getName().contains(imageName) && f.getName().endsWith("-mask.png"))){
                files.remove(f);
            }
        }

        if (files.isEmpty()) {
            Dialogs.showErrorMessage(title, "No valid files found in directory!");
            return;
        }

        // Create annotations for all the files
        Collection<PathObject> annotations = new ArrayList<>();
        for (File f : files){
            try{
                annotations.add(writePathObjectFromMask(f));
            }
            catch (Exception ex){
                Dialogs.showErrorNotification("Could not write binary mask as annotations | File name: " + f.getName(), ex.getLocalizedMessage());
            }
        }

        String annString = "annotation" + (annotations.size()>1 ? "s" : "");

        // Confirm that user wants to import annotations as binary masks
		GridPane pane = new GridPane();
		PaneTools.addGridRow(pane, 0, 0, null, new Label(String.format("%d %s will be imported from the /" + tf.getText() + " directory to:", 
        annotations.size(), annString)));
		PaneTools.addGridRow(pane, 1, 0, null, new Label(projectPath.toString()));
		boolean confirmImport = Dialogs.showConfirmDialog("Import " + annString + " from project?", pane);

		if (confirmImport){
			// Export annotations
            hierarchy.addPathObjects(annotations);
			
			Dialogs.showInfoNotification(StringUtils.capitalize(annString) + " imported successfully", String.format("%d %s %s successfully imported from "+tf.getText(), 
			annotations.size(), 
					annString, 
					(annotations.size() == 1 ? "was" : "were")));
		}
        
           
    }

}