package org.reactome.server.tools.fireworks.exporter.raster.renderers;

import org.reactome.server.analysis.core.model.AnalysisType;
import org.reactome.server.tools.fireworks.exporter.common.profiles.ColorFactory;
import org.reactome.server.tools.fireworks.exporter.common.profiles.FireworksColorProfile;
import org.reactome.server.tools.fireworks.exporter.raster.index.FireworksAnalysis;
import org.reactome.server.tools.fireworks.exporter.raster.index.FireworksIndex;
import org.reactome.server.tools.fireworks.exporter.raster.index.Node;
import org.reactome.server.tools.fireworks.exporter.raster.layers.FireworksCanvas;
import org.reactome.server.tools.fireworks.exporter.raster.layers.RegulationSheet;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * Renders nodes, that's why its called {@link NodeRenderer}
 */
public class NodeRenderer {

	private static final double MIN_NODE_SIZE = 0.025;
	private static final int NODE_FACTOR = 18; // default 18
	private static final Stroke SELECTION_STROKE = new BasicStroke(1);
	private static final Stroke FLAG_STROKE = new BasicStroke(2);
	private final FireworksColorProfile profile;
	private final FireworksIndex index;
	private final FireworksCanvas canvas;

	public NodeRenderer(FireworksColorProfile profile, FireworksIndex index, FireworksCanvas canvas) {
		this.profile = profile;
		this.index = index;
		this.canvas = canvas;
	}

	/**
	 * renders node into canvas
	 */
	public void render(Node node, int t) {
		final double diameter = (node.getFireworksNode().getRatio() + MIN_NODE_SIZE) * NODE_FACTOR;
		final double x = node.getFireworksNode().getX() - diameter * 0.5;
		final double y = node.getFireworksNode().getY() - diameter * 0.5;
		final Shape ellipse = new Ellipse2D.Double(x, y, diameter, diameter);

		draw(node, ellipse, t);
		if (node.isSelected()) selection(ellipse);
		if (node.isFlag()) flag(ellipse);
		text(node);
	}

	public void render(Node node) {
		render(node, 0);
	}

	private void draw(Node node, Shape ellipse, int t) {
		final Color color = getNodeColor(node, t);
		canvas.getNodes().add(ellipse, color);
	}

	private Color getNodeColor(Node node, int t) {
		if (index.getAnalysis().getResult() == null)
			return profile.getNode().getInitial();
		if (index.getArgs().getCoverage()) {
			final Double c = index.getAnalysis().getCoverage(node);
			if (c != null)
				return ColorFactory.interpolate(profile.getNode().getEnrichment(), c);
		} else if (index.getAnalysis().getType() == AnalysisType.EXPRESSION
				|| index.getAnalysis().getType() == AnalysisType.GSA_STATISTICS
				|| index.getAnalysis().getType() == AnalysisType.GSVA) {
			if (node.getExp() != null) {
				if (node.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
					final double min = index.getAnalysis().getSpeciesResultFiltered().getExpressionSummary().getMin();
					final double max = index.getAnalysis().getSpeciesResultFiltered().getExpressionSummary().getMax();
					final double gui = node.getExp().get(t);
					final double val = 1 - (gui - min) / (max - min);
					return ColorFactory.interpolate(profile.getNode().getExpression(), val);
				}
				return profile.getNode().getHit();
			}
		} else if (index.getAnalysis().getType() == AnalysisType.GSA_REGULATION) {
			if (node.getExp() != null) {
				if (node.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
					RegulationSheet sheet = new RegulationSheet(profile.getNode().getExpression());
					return sheet.getColorMap().get(node.getExp().get(t).intValue());
				} else {
					return profile.getNode().getHit();
				}
			}
		} else if (index.getAnalysis().getType() == AnalysisType.OVERREPRESENTATION
				|| index.getAnalysis().getType() == AnalysisType.SPECIES_COMPARISON) {
			if (node.getpValue() != null && node.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
				final double val = node.getpValue() / FireworksAnalysis.P_VALUE_THRESHOLD;
				return ColorFactory.interpolate(profile.getNode().getEnrichment(), val);
			}
		}
		return profile.getNode().getFadeout();
	}

	private void selection(Shape ellipse) {
		canvas.getNodeSelection().add(ellipse, profile.getNode().getSelection(), SELECTION_STROKE);
	}

	private void flag(Shape ellipse) {
		canvas.getNodeFlags().add(ellipse, profile.getNode().getFlag(), FLAG_STROKE);
	}

	private void text(Node node) {
		if (node.isTopLevel()) {
			final Color color = node.isSelected()
					? Color.BLUE
					: Color.BLACK;
			canvas.getText().add(node.getFireworksNode().getName(),
					new Point2D.Double(node.getFireworksNode().getX(),
							node.getFireworksNode().getY()), color);
		}
		// Draw only nodes explicitly selected
		if (node.isSelected() && index.getDecorator().getSelected().contains(node.getFireworksNode().getDbId())) {
			canvas.getText().add(node.getFireworksNode().getName(),
					new Point2D.Double(node.getFireworksNode().getX(),
							node.getFireworksNode().getY()), Color.BLUE);
		}
	}

}
