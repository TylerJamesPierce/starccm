// batch STAR-CCM+ macro with post processing
// Set the directoryPost!
// Tyler Pierce
package macro;

import java.util.*;
import java.io.*;
import java.nio.*;
import star.flow.*;
import star.base.neo.*;   // for use with version >= 1.09
import star.base.report.*;
import star.common.*;
import star.coremodule.services.*;
import star.saturb.*;
import star.keturb.*;
import star.kwturb.*;
//import star.scenefile.*;
import star.common.StarPlot.*;
import star.vis.*;
//import star.segregatedenergy.*;
//import star.segregatedflow.*;
import star.coupledflow.*;
import star.meshing.*;

public class MyTempDriver extends StarMacro {
   //declare global variables
   String directoryPost = "/home/aa411/2014/tyler/ss/reve/results/";
   Simulation sim;
   String currentDirectory;
   String simName;
   BufferedWriter bwout = null;
   String[] monitorPlots = {"ForceCoefficientsWindAxis","MomentCoefficientsWindAxis25"};
   String[] scenes = {"WingFenceVelocityMagnitude","WingRootVelocityMagnitude","WingTipVelocityMagnitude","YPlus3D","CenterlineVelocityMagnitude","CenterlineVelocityVectorField","PressureCoefficient3D","TurbulenceIsoSurface3D","Streamlines"};
   String[] forceTables = {"Accumulated Lift Table All Walls Streamwise","Accumulated Drag Table All Walls Spanwise","Accumulated Drag Table All Walls Streamwise","Accumulated Lift Table All Walls Streamwise","Accumulated Lift Table Wing Only Spanwise","Accumulated Lift Table Wing Only Streamwise"};
   
   public void execute() {
    try {
      //initialize global variables
      sim = getActiveSimulation();
      currentDirectory = sim.getSessionDir();
      simName = sim.getPresentationName();  
      bwout = new BufferedWriter(new FileWriter(resolvePath(directoryPost + simName + "_report.csv")));
      bwout.write("Report Name, Value, Unit, \n");
      
      SimulationIterator simIterator = sim.getSimulationIterator();
      
      //Mesh the current simulation if necessary
      MeshPipelineController meshPipelineController_0 = 
              sim.get(MeshPipelineController.class);

      meshPipelineController_0.generateVolumeMesh();
      
       //Get Volume Mesh:
      FvRepresentation volumeMesh =
            ((FvRepresentation) sim.getRepresentationManager().getObject("Volume Mesh"));

      // (COUPLED SOLVER ONLY): Courant number
      CoupledImplicitSolver coupledImplicitSolver_0 =
            ((CoupledImplicitSolver) sim.getSolverManager().getSolver(CoupledImplicitSolver.class));

      coupledImplicitSolver_0.setCFL($CFL);

      //Setup Alpha and Beta

      Region region_0 =
            sim.getRegionManager().getRegion("Domain");
      // Adjusts the Wind Axis Coordinate System for the AOA

      LabCoordinateSystem labCordSystm = 
            ((LabCoordinateSystem) sim.getCoordinateSystemManager().getLabCoordinateSystem());

      CartesianCoordinateSystem windAxis = 
            ((CartesianCoordinateSystem) labCordSystm.getLocalCoordinateSystemManager().getObject("WindAxis"));

      windAxis.setBasis0(new DoubleVector(new double[] {1.0, 0.0, 0.0}));

      // Max steps
      StepStoppingCriterion stepStopper =
            ((StepStoppingCriterion) sim.getSolverStoppingCriterionManager().getSolverStoppingCriterion("Maximum Steps"));
      
      stepStopper.setMaximumNumberSteps($MaxSteps);

        // RUN until other stopping criteria are met
      simIterator.run();

      Collection<Report>  reports = sim.getReportManager().getObjects();
      try {
        for (Report thisReport : reports) {
          thisReport.getReportManager().applyRepresentation(volumeMesh);
          String fieldLocationName = thisReport.getPresentationName();
          Double fieldValue = thisReport.getReportMonitorValue();
          String fieldUnits = thisReport.getUnits().toString();
          // Printing to check in output window
          sim.println(" Field Location: " + fieldLocationName);
          sim.println(" Field Value: " + fieldValue);
          sim.println(" Field Units: " + fieldUnits);
          bwout.append( fieldLocationName + ", "+fieldValue+", "+fieldUnits+"\n");
          //bwout.write( fieldLocationName + ", "+fieldValue+", "+fieldUnits+"\n");
          //out.append(r.getPresentationName() + "," + r.getReportMonitorValue() + "\n");
        }
        bwout.close();
      } catch (IOException ex) {
          sim.println("Error Writing Output File!");
          sim.println(ex.getClass().getName());
      }

      //Save all plots
      //Save monitor plots
      for (String monitorPlot : monitorPlots) {
          MonitorPlot mp = ((MonitorPlot) sim.getPlotManager().getPlot(monitorPlot));
          mp.encode(directoryPost+simName+monitorPlot+".png");
      }
      //Save residual plots
      ResidualPlot residualPlot = 
            ((ResidualPlot) sim.getPlotManager().getPlot("Residuals"));
      residualPlot.encode(directoryPost+simName+residualPlot.getPresentationName()+".png");
   
      //Get vector of all scenes:
      Collection<Scene> colSCN = sim.getSceneManager().getScenes();

      if (!colSCN.isEmpty()) {//Make sure scenes exist
         for (Scene sce : colSCN){//Save all scenes
            CurrentView currentView = sce.getCurrentView();
            sce.getDisplayerManager().setRepresentation(volumeMesh);
            for (String thisScene : scenes) { //check if collection matches string defined at top of file
                if (sce.getPresentationName().equalsIgnoreCase(thisScene)) {
                    sce.printAndWait(directoryPost+simName+sce+".png", 1);
                } 
            }
            
            if (sce.getPresentationName().endsWith("2V")) { //save multiple views
                LabCoordinateSystem labCS = sim.getCoordinateSystemManager().getLabCoordinateSystem();
                currentView.setCoordinateSystem(labCS);
                currentView.setInput(new DoubleVector(new double[] {0.3742647157952055, 6.645032671368789E-4, -0.11884133289981}), new DoubleVector(new double[] {-0.930348673026338, 0.08317114114103613, 0.13116652850372193}), new DoubleVector(new double[] {0.1878009647194752, -0.006620871470954015, 0.9821847900020646}), 0.3474378117482561, 0);
                sce.printAndWait(directoryPost+simName+sce+"view1.png", 1);
                currentView.setCoordinateSystem(labCS);
                currentView.setInput(new DoubleVector(new double[] {1.2978575525898077, -0.6829935572126207, 0.21536757784691607}), new DoubleVector(new double[] {-3.020614659596681, -0.558892249369588, 2.5805068888308664}), new DoubleVector(new double[] {0.4488276085480654, 0.4012835666438491, 0.7984518000146386}), 1.2857588511608762, 0);
                sce.printAndWait(directoryPost+simName+sce+"view2.png", 1);
            }
            
            if (sce.getPresentationName().endsWith("3D")) { //save 3D scene
                sce.export3DSceneFileAndWait(resolvePath(directoryPost+simName+sce+".sce"), true);
            }
        }
    }
    
    for (String thisTable : forceTables) {
            AccumulatedForceTable accumulatedForceTable = 
      ((AccumulatedForceTable) sim.getTableManager().getTable(thisTable));
            accumulatedForceTable.extract();
            accumulatedForceTable.export(resolvePath(directoryPost+simName+thisTable+".csv"), ",");
    }

    } catch (Exception e) {
      sim.println("Error during analysis");
    }
    finally {
       File f = new File(currentDirectory + System.getProperty("file.separator") + "DONE");
       try {
           f.createNewFile();
       } catch (IOException ex) {
       }
   }
   }
}
                                                                         
