package com.csgt.controller.tasks;

import com.csgt.controller.ControllerUtil;
import com.csgt.dataaccess.DAO.EdgeDAOImpl;
import com.csgt.dataaccess.DAO.ElementDAOImpl;
import com.csgt.dataaccess.DAO.ElementToChildDAOImpl;
import com.csgt.dataaccess.DTO.EdgeDTO;
import com.csgt.dataaccess.DTO.ElementDTO;
import com.csgt.dataaccess.DTO.ElementToChildDTO;
import com.csgt.dataaccess.model.EdgeElement;
import com.csgt.dataaccess.model.Element;
import com.csgt.controller.files.parsers.ParseCallTrace;
import com.csgt.controller.files.FileNames;
import com.csgt.controller.files.LoadedFiles;
import com.csgt.controller.modules.ElementTreeModule;
import com.csgt.controller.modules.ModuleLocator;
import javafx.concurrent.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This task handles the creation of in-memory tree and insertion of the corresponding records to the database.
 */
public class ConstructTreeTask extends Task<Void> {

    private File callTraceLogFile;
    private ElementTreeModule elementTreeModule;

    public ConstructTreeTask() {
        this.callTraceLogFile = LoadedFiles.getFile(FileNames.Call_Trace.getFileName());
        elementTreeModule = ModuleLocator.getElementTreeModule();
    }

    @Override
    protected Void call() {
        computeAndInsertElements();
        computeAndInsertEdges();

        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
    }

    private void computeAndInsertEdges() {
        List<EdgeElement> edgeElementList = new ArrayList<>();
        ModuleLocator.getElementTreeModule().recursivelyInsertEdgeElementsIntoDB(
                ElementTreeModule.greatGrandParent,
                edgeElementList
        );

        List<EdgeDTO> edgeDTOList = ControllerUtil.convertEdgeElementsToEdgeDTO(edgeElementList);
        EdgeDAOImpl.insert(edgeDTOList);
    }

    private void computeAndInsertElements() {
        LinesInserted linesInserted = new LinesInserted(
                0,
                2 * ParseCallTrace.countNumberOfLines(callTraceLogFile)
        );

        updateTitle("Writing to DB.");
        updateMessage("Please wait... total records: " + linesInserted.total + " records processed: " + linesInserted.insertedSoFar);
        updateProgress(linesInserted.insertedSoFar, linesInserted.total);

        elementTreeModule.calculateElementProperties();
        Element root = ElementTreeModule.greatGrandParent;

        if (root == null)
            return;

        Queue<Element> queue = new LinkedList<>();
        queue.add(root);

        List<Element> elementList = new ArrayList<>();
        List<ElementToChildDTO> elementToChildDTOList = new ArrayList<>();

        Element element;
        while ((element = queue.poll()) != null) {
            elementList.add(element);

            ElementToChildDTO elementToChildDTO = new ElementToChildDTO();
            elementToChildDTO.setParentId(String.valueOf(element.getParent() == null ? -1 : element.getParent().getElementId()));
            elementToChildDTO.setChildId(String.valueOf(element.getElementId()));

            elementToChildDTOList.add(elementToChildDTO);

            // ElementDAOImpl.insert(element);
            // ElementToChildDAOImpl.insert(
            //         element.getParent() == null ? -1 : element.getParent().getElementId(),
            //         element.getElementId());

            if (element.getChildren() != null) {
                queue.addAll(element.getChildren());
            }

            linesInserted.insertedSoFar++;
            updateMessage("Please wait... total records: " + linesInserted.total + " records processed: " + linesInserted.insertedSoFar);
            updateProgress(linesInserted.insertedSoFar, linesInserted.total);
        }

        List<ElementDTO> elementDTOList = ControllerUtil.convertElementToElementDTO(elementList);
        ElementDAOImpl.insert(elementDTOList);
        ElementToChildDAOImpl.insert(elementToChildDTOList);
    }

    public class LinesInserted {
        long insertedSoFar;
        long total;

        LinesInserted(long insertedSoFar, long totalBytes) {
            this.insertedSoFar = insertedSoFar;
            this.total = totalBytes;
        }
    }
}
