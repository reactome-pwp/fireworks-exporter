package org.reactome.server.tools.fireworks.exporter.raster.renderers;

import org.reactome.server.analysis.core.model.AnalysisType;
import org.reactome.server.tools.fireworks.exporter.common.profiles.ColorFactory;
import org.reactome.server.tools.fireworks.exporter.common.profiles.FireworksColorProfile;
import org.reactome.server.tools.fireworks.exporter.raster.index.Edge;
import org.reactome.server.tools.fireworks.exporter.raster.index.FireworksAnalysis;
import org.reactome.server.tools.fireworks.exporter.raster.index.FireworksIndex;
import org.reactome.server.tools.fireworks.exporter.raster.layers.FireworksCanvas;
import org.reactome.server.tools.fireworks.exporter.raster.layers.RegulationSheet;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Renders edges.
 */
public class EdgeRenderer {

	private static final Stroke DEFAULT_STROKE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
	private static final Stroke SELECTION_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
	private static final Stroke FLAG_STROKE = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
	private final FireworksColorProfile profile;
	private final FireworksIndex index;
	private final FireworksCanvas canvas;

	/**
	 * Creates an EdgeRenderer.
	 */
	public EdgeRenderer(FireworksColorProfile profile, FireworksIndex index, FireworksCanvas canvas) {
		this.profile = profile;
		this.index = index;
		this.canvas = canvas;
	}

	/**
	 * Renders edge into canvas.
	 */
	public void render(Edge edge, int t) {
		final Path2D path = new Path2D.Double();
		path.moveTo(edge.getFrom().getFireworksNode().getX(), edge.getFrom().getFireworksNode().getY());

		double dX = edge.getTo().getFireworksNode().getX() - edge.getFrom().getFireworksNode().getX();
		double dY = edge.getTo().getFireworksNode().getY() - edge.getFrom().getFireworksNode().getY();
		double angle = Math.atan2(dY, dX) - (Math.PI / 6);
		double r = Math.sqrt(dX * dX + dY * dY) * 3 / 5.0;
		double x = edge.getFrom().getFireworksNode().getX() + r * Math.cos(angle);
		double y = edge.getFrom().getFireworksNode().getY() + r * Math.sin(angle);

		path.quadTo(x, y, edge.getTo().getFireworksNode().getX(), edge.getTo().getFireworksNode().getY());

		draw(edge, path, t);
		if (edge.isSelected()) selection(path);
		if (edge.isFlag()) flag(path);
	}

	public void render(Edge edge) {
		render(edge, 0);
	}

	private void draw(Edge edge, Path2D path, int t) {
		final Color color = getEdgeColor(edge, t);
		canvas.getEdges().add(path, color, DEFAULT_STROKE);
	}

	private Color getEdgeColor(Edge edge, int t) {
		if (index.getAnalysis().getResult() == null)
			return profile.getEdge().getInitial();
		if(index.getArgs().getCoverage()) {
			final Double c = index.getAnalysis().getCoverage(edge.getTo());
			if (c != null)
				return ColorFactory.interpolate(profile.getNode().getEnrichment(), c);
		} else if (index.getAnalysis().getType() == AnalysisType.EXPRESSION
				|| index.getAnalysis().getType() == AnalysisType.GSA_STATISTICS
				|| index.getAnalysis().getType() == AnalysisType.GSVA) {
			if (edge.getTo().getExp() != null) {
				if (edge.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
					final double min = index.getAnalysis().getSpeciesResultFiltered().getExpressionSummary().getMin();
					final double max = index.getAnalysis().getSpeciesResultFiltered().getExpressionSummary().getMax();
					final double val = 1 - (edge.getTo().getExp().get(t) - min) / (max - min);
					return ColorFactory.interpolate(profile.getNode().getExpression(), val);
				}
				return profile.getNode().getHit();
			}
		} else if (index.getAnalysis().getType() == AnalysisType.GSA_REGULATION) {
			if (edge.getTo().getExp() != null) {
				if (edge.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
					RegulationSheet sheet = new RegulationSheet(profile.getEdge().getExpression());
					return sheet.getColorMap().get(edge.getTo().getExp().get(t).intValue());
				} else {
					return profile.getEdge().getHit();
				}
			}
		} else if (index.getAnalysis().getType() == AnalysisType.OVERREPRESENTATION
				|| index.getAnalysis().getType() == AnalysisType.SPECIES_COMPARISON) {
			if (edge.getpValue() != null && edge.getpValue() <= FireworksAnalysis.P_VALUE_THRESHOLD) {
				final double val = edge.getpValue() / FireworksAnalysis.P_VALUE_THRESHOLD;
				return ColorFactory.interpolate(profile.getEdge().getEnrichment(), val);
			}
		}
		return profile.getEdge().getFadeout();
	}

	private void selection(Path2D path) {
		canvas.getEdgeSelection().add(path, profile.getEdge().getSelection(), SELECTION_STROKE);
	}

	private void flag(Path2D path) {
		canvas.getEdgeFlags().add(path, profile.getEdge().getFlag(), FLAG_STROKE);
	}

}
