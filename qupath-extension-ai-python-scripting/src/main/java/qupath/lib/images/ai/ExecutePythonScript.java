package qupath.lib.images.ai;

import qupath.lib.projects.Project;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.PaneTools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;


public final class ExecutePythonScript implements Runnable {

    private QuPathGUI qupath;
	
	private final String title = "Run Python Script";

    ExecutePythonScript(QuPathGUI qupath) {
		this.qupath = qupath;
	}

    // Gets Path object from specified String object
    private static Path rpath(String pathString){
        return Paths.get(pathString);
    }

    // Makes sure necessary files exist to set up Python environment and run Python scripts
    private static boolean necessaryFilesExist(Path projectFolderPath){
        boolean allExist = true;

        String pythonScriptsPath = projectFolderPath.toString() + "\\" + "python";

        String[] subPaths = {"\\cmds\\createEnv.bat", "\\cmds\\activate.bat", 
            "\\cmds\\loadReqs.bat", "\\cmds\\setUpPy.bat", "\\scripts"};

        for (String subpath : subPaths){
            allExist = allExist && Files.exists(rpath(pythonScriptsPath+subpath));
        }

        return allExist;
    }

    @Override
    public void run(){
        
        Project<BufferedImage> project = qupath.getProject();

        Path projectFolderPath = project.getPath().getParent();

        // Create setup file to execute script
        try{
            if (Files.exists(rpath(projectFolderPath+"\\python\\cmds\\setUpPy.bat"))){
                File toDelete = new File(projectFolderPath+"\\python\\cmds\\setUpPy.bat");
                toDelete.delete();
            }

            // Directory Selection
            GridPane pythonFileField = new GridPane();
            pythonFileField.setVgap(5.0);
            TextField tf = new TextField();
            tf.setPrefWidth(400);

            PaneTools.addGridRow(pythonFileField, 0, 0, "Enter file path", new Label("Enter the path of the Python file you'd like to run "+
                "(relative to your /scripts folder)"));
            PaneTools.addGridRow(pythonFileField, 1, 0, "Enter file path", new Label("(Ex. main.py)"));
            PaneTools.addGridRow(pythonFileField, 2, 0, "Enter file path", tf, tf);

            boolean confirmDirectory = Dialogs.showConfirmDialog("Enter file path", pythonFileField);
		    if (!confirmDirectory)
		        return;

            if (!Files.exists(rpath(projectFolderPath+"\\python\\scripts\\"+tf.getText()))){
                Dialogs.showErrorMessage(title, "File specified does not exist. Make sure your Python file is contained in a /scripts folder,"+
                " within your project folder.");
                return;
            }

            File setUpPy = new File(projectFolderPath+"\\python\\cmds\\setUpPy.bat");   
            FileWriter writer = new FileWriter(projectFolderPath+"\\python\\cmds\\setUpPy.bat");
                
            String[] commandStrings = {"set projectFolderPath="+projectFolderPath.toString(),
                "call "+projectFolderPath.toString()+"\\python\\cmds\\createEnv.bat", // create virtual environment to avoid dependency clashing
                "call "+projectFolderPath.toString()+"\\python\\cmds\\activate.bat", // activate virtual environment
                "call "+projectFolderPath.toString()+"\\python\\cmds\\loadReqs.bat", // load libraries necessary to run python file
                "python "+projectFolderPath.toString()+"\\python\\scripts\\"+tf.getText(), // run python file
                "deactivate"}; // deactivate virtual environment

            for (String command : commandStrings){
                    writer.write(command);
                    writer.write(System.lineSeparator());
            }

            writer.close();
        }

        catch (IOException ex){
                Dialogs.showErrorNotification("Unable to set up required file", ex.getLocalizedMessage());
        }


        // Execute script if all necessary files exist
        if (necessaryFilesExist(projectFolderPath)){
            Runtime command = Runtime.getRuntime();
            try {
                Process createPythonAction = command.exec("cmd /c start "+projectFolderPath+"\\python\\cmds\\setUpPy.bat");
                createPythonAction.waitFor();
            }

            catch(IOException ex) {
                Dialogs.showErrorNotification("Could not set up Python environment", ex.getLocalizedMessage());
            }

            catch(InterruptedException ex){
                Dialogs.showErrorNotification("Python environment setup was interrupted", ex.getLocalizedMessage());
            }
        }

        else{
            Dialogs.showErrorMessage(title, "Required files not found in directory: "+projectFolderPath.toString());
        }

    }
}


