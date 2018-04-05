package com.application.service.tasks;

import com.application.db.DAO.DAOImplementation.CallTraceDAOImpl;
import com.application.db.DAO.DAOImplementation.MethodDefnDAOImpl;
import com.application.db.DatabaseUtil;
import com.application.logs.fileHandler.CallTraceLogFile;
import com.application.logs.fileHandler.MethodDefinitionLogFile;
import com.application.logs.fileIntegrity.CheckFileIntegrity;
import com.application.logs.parsers.ParseCallTrace;
import com.application.logs.parsers.ParseMethodDefinition;
import com.application.service.files.FileNames;
import com.application.service.files.LoadedFiles;
import com.application.service.modules.ModuleLocator;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.SQLException;
import java.util.List;

public class ParseFileTask extends Task<Void> {

    private File methodDefinitionLogFile;
    private File callTraceLogFile;

    public ParseFileTask() {
        this.methodDefinitionLogFile = LoadedFiles.getFile(FileNames.METHOD_DEF.getFileName());
        this.callTraceLogFile = LoadedFiles.getFile(FileNames.Call_Trace.getFileName());
    }

    @Override
    protected Void call() {
        // Reset Database
        updateTitle("Resetting the Database.");
        DatabaseUtil.resetDB();

        BytesRead bytesRead = new BytesRead(
                0,
                methodDefinitionLogFile.length() + 2 * callTraceLogFile.length()
        );
        // Method Definition log file integrity check.
        updateTitle("Checking integrity of Method Definition log file.");
        updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
        updateProgress(bytesRead.readSoFar, bytesRead.total);

        CheckFileIntegrity.checkFile(LoadedFiles.getFile(FileNames.Call_Trace.getFileName()), bytesRead, (Void) -> {
            updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
            updateProgress(bytesRead.readSoFar, bytesRead.total);
        });

        // Parse Method Definition Log files.
        updateTitle("Parsing log files.");
        updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
        // updateProgress(bytesRead.readSoFar, bytesRead.total);

        List<List<String>> parsedMDLineList = new ParseMethodDefinition().parseFile(
                LoadedFiles.getFile(FileNames.METHOD_DEF.getFileName()),
                bytesRead,
                (Void) -> {
                    updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
                    updateProgress(bytesRead.readSoFar, bytesRead.total);
                });

        MethodDefnDAOImpl.insertList(parsedMDLineList);

        // new ParseMethodDefinition().readFile(MethodDefinitionLogFile.getFile(), bytesRead, parsedLineList -> {
        //     MethodDefnDAOImpl.insert(parsedLineList);
        //     updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
        //     updateProgress(bytesRead.readSoFar, bytesRead.total);
        // });

        new ParseCallTrace().readFile(
                LoadedFiles.getFile(FileNames.Call_Trace.getFileName()),
                bytesRead,
                parsedLineList -> {
                    try {
                        int autoIncrementedId = CallTraceDAOImpl.insert(parsedLineList);
                        ModuleLocator.getElementTreeModule().StringToElementList(parsedLineList, autoIncrementedId);
                        updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
                        updateProgress(bytesRead.readSoFar, bytesRead.total);
                    } catch (SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {  // Todo Create a custom exception class and clean this.
                        e.printStackTrace();
                    }
                });

        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
    }

    public class BytesRead {
        public long readSoFar;
        long total;

        BytesRead(long readSoFar, long totalBytes) {
            this.readSoFar = readSoFar;
            this.total = totalBytes;
        }
    }
}
