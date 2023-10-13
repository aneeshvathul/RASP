package qupath.lib.images.ai;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.nio.file.Path;

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
import qupath.lib.objects.PathObject;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.projects.Project;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.RoiTools;

import javax.imageio.ImageIO;
import java.awt.Color;


public final class BinaryMaskExport implements Runnable {

    private QuPathGUI qupath;
	
	private final String title = "Export Annotations as Binary Masks";

    BinaryMaskExport(QuPathGUI qupath) {
		this.qupath = qupath;
	}

    private static void writeMaskFromPathObject(PathObject pathObject, String pathOutput, ImageServer<BufferedImage> server, 
             double downsample){

            // Extract ROI & classification name
            ROI roi = pathObject.getROI();
            PathClass pathClass = pathObject.getPathClass();
            String classificationName = pathClass == null ? "None" : pathClass.toString();
            
            if (roi == null) {
                return;
            }

            // Create a region from the ROI
            RegionRequest region = RegionRequest.createInstance(server.getPath(), downsample, roi);
            
            // Create a name
            String name = String.format("%s_%s_(%.2f,%d,%d,%d,%d)",
                    server.getMetadata().getName(),
                    classificationName,
                    region.getDownsample(),
                    region.getX(),
                    region.getY(),
                    region.getWidth(),
                    region.getHeight()
                );

            
            try{

                // Request the BufferedImage
                BufferedImage img = server.readBufferedImage(region);

                // Create a mask using Java2D functionality
                // (This involves applying a transform to a graphics object, so that none needs to be applied to the ROI coordinates)
                var shape = RoiTools.getShape(roi);
                BufferedImage imgMask = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

                var g2d = imgMask.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.scale(1.0/downsample, 1.0/downsample);
                g2d.translate(-region.getX(), -region.getY());
                g2d.fill(shape);
                g2d.dispose();

                // Export the mask
                File fileMask = new File(pathOutput, name + "-mask.png");
                ImageIO.write(imgMask, "PNG", fileMask);

            }

            catch (IOException ex){
                Dialogs.showErrorNotification("Could not write annotations as binary masks", ex.getLocalizedMessage());
            }

    }

    @Override
    public void run(){

        QuPathViewer viewer = qupath.getViewer();
		ImageServer<BufferedImage> server = viewer.getServer();
        Project<BufferedImage> project = qupath.getProject();

        Collection<PathObject> annotations = viewer.getHierarchy().getAnnotationObjects();

        // Directory Selection
        GridPane maskFolderField = new GridPane();
		maskFolderField.setVgap(5.0);
		TextField tf = new TextField();
		tf.setPrefWidth(400);

		PaneTools.addGridRow(maskFolderField, 0, 0, "Enter folder path", new Label("Enter the relative folder path (inside your project folder)"+
        " that you would like your binary masks to be exported to."));
        PaneTools.addGridRow(maskFolderField, 1, 0, "Enter folder path", new Label("(If the relative path does not exist, it will be created)"));
		PaneTools.addGridRow(maskFolderField, 2, 0, "Enter folder path", tf, tf);

		boolean confirmDirectory = Dialogs.showConfirmDialog("Enter folder path", maskFolderField);
		if (!confirmDirectory)
		    return;

        Path projectPath = project.getPath();
        String pathOutput = projectPath.getParent().toString() + "\\" + tf.getText();

        double downsample = 1.0;


        /*  Creates folder in project parent directory for binary masks to be exported, 
            (or exports to the folder if it already exists)
        */
        boolean maskFolderCreation = new File(pathOutput).mkdirs();

        String annString = "annotation" + (annotations.size()>1 ? "s" : "");

        // Confirm that user wants to export annotations as binary masks
		GridPane pane = new GridPane();
		PaneTools.addGridRow(pane, 0, 0, null, new Label(String.format("%d %s will be exported as binary masks to the /" + tf.getText() + " directory from:", 
        annotations.size(), annString)));
		PaneTools.addGridRow(pane, 1, 0, null, new Label(projectPath.toString()));
		boolean confirmExport = Dialogs.showConfirmDialog("Export " + annString + " from project?", pane);

		if (confirmExport){
			// Export annotations

            for (PathObject ann : annotations){
                writeMaskFromPathObject(ann, pathOutput, server, downsample);
            }
			
			Dialogs.showInfoNotification(StringUtils.capitalize(annString) + " exported successfully", String.format("%d %s %s successfully exported from project", 
			annotations.size(), 
					annString, 
					(annotations.size() == 1 ? "was" : "were")));
		}
	
    }
}