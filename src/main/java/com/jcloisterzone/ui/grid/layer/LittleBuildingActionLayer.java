package com.jcloisterzone.ui.grid.layer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jcloisterzone.action.LittleBuildingAction;
import com.jcloisterzone.board.Position;
import com.jcloisterzone.board.Rotation;
import com.jcloisterzone.game.Token;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.ImmutablePoint;
import com.jcloisterzone.ui.controls.action.ActionWrapper;
import com.jcloisterzone.ui.grid.ActionLayer;
import com.jcloisterzone.ui.grid.GridMouseAdapter;
import com.jcloisterzone.ui.grid.GridMouseListener;
import com.jcloisterzone.ui.grid.GridPanel;

public class LittleBuildingActionLayer extends AbstractGridLayer implements ActionLayer, GridMouseListener {

    protected static final Composite SHADOW_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .4f);
    private static final double PADDING_RATIO = 0.10;

    private Map<Token, Image> images = new HashMap<>();
    private ActionWrapper actionWrapper;
    //private LittleBuildingAction action;
    private Token selected = null;

    private HashMap<Token, Rectangle> areas = new HashMap<>();

    int icoSize, padding;

    public LittleBuildingActionLayer(GridPanel gridPanel, GameController gc) {
        super(gridPanel, gc);
        images.put(Token.LB_SHED, rm.getImage("neutral/lb_shed"));
        images.put(Token.LB_HOUSE, rm.getImage("neutral/lb_house"));
        images.put(Token.LB_TOWER, rm.getImage("neutral/lb_tower"));
        recomputeDimenensions(getTileWidth());
    }


    @Override
    public void setActionWrapper(boolean active, ActionWrapper actionWrapper) {
        this.actionWrapper = actionWrapper;
        if (active) {
            prepareAreas();
        } else {
            selected = null;
            areas.clear();
        }
    }

    @Override
    public ActionWrapper getActionWrapper() {
        return actionWrapper;
    }

    @Override
    public LittleBuildingAction getAction() {
        return actionWrapper == null ? null : (LittleBuildingAction) actionWrapper.getAction();
    }

    private void recomputeDimenensions(int tileWidth) {
        icoSize = tileWidth / 2;
        padding = (int) (icoSize * PADDING_RATIO);
    }


    @Override
    public void zoomChanged(int tileWidth) {
        recomputeDimenensions(tileWidth);
        areas.clear();
        prepareAreas();
        super.zoomChanged(tileWidth);
    }

    @Override
    public void boardRotated(Rotation boardRotation) {
        areas.clear();
        prepareAreas();
        super.boardRotated(boardRotation);
    }

    private int geBuildingIndex(Token lb) {
        if (lb == Token.LB_SHED) return 0;
        if (lb == Token.LB_HOUSE) return 1;
        if (lb == Token.LB_TOWER) return 2;
        throw new IllegalArgumentException();
    }

    private int getIconX(Token lb) {
        int x = -icoSize / 2 - padding * 3;
        return x + geBuildingIndex(lb) * (icoSize + 3*padding);
    }

    @SuppressWarnings("unused")
    private int getIconY(Token lb) {
        return -icoSize / 2;
    }


    private void prepareAreas() {
        LittleBuildingAction action = getAction();
        if (action == null) return;

        Position pos = action.getPosition();
        AffineTransform at = getAffineTransformIgnoringRotation(pos);

        for (Token lb : Token.littleBuildingValues()) {
            if (action.getOptions().contains(lb)) {
                int x = getIconX(lb), y = getIconY(lb);
                Rectangle rect = new Rectangle(x-padding, y-padding, icoSize+2*padding, icoSize+2*padding);
                areas.put(lb, at.createTransformedShape(rect).getBounds());
            }
        }
    }

    @Override
    public void paint(Graphics2D g2) {
        LittleBuildingAction action = getAction();
        Position pos = action.getPosition();

        Composite origComposite = g2.getComposite();
        ImmutablePoint shadowOffset = new ImmutablePoint(3, 3);
        shadowOffset = shadowOffset.rotate(gridPanel.getBoardRotation().inverse());

        for (Token lb : Token.littleBuildingValues()) {
            Rectangle r = areas.get(lb);
            if (r == null) continue;
            g2.setComposite(SHADOW_COMPOSITE);
            g2.setColor(Color.BLACK);
            g2.fillRect(r.x+shadowOffset.getX(), r.y+shadowOffset.getY(), r.width, r.height);
            g2.setComposite(origComposite);
            g2.setColor(selected == lb ? Color.WHITE : Color.GRAY);
            g2.fill(r);

            Image img = images.get(lb);
            int x = getIconX(lb), y = getIconY(lb);
            AffineTransform at = getAffineTransform(pos, gridPanel.getBoardRotation().inverse());
            at.concatenate(AffineTransform.getTranslateInstance(x, y));
            at.concatenate(AffineTransform.getScaleInstance(icoSize / (double) img.getWidth(null), icoSize / (double) img.getHeight(null)));
            g2.drawImage(images.get(lb), at, null);
        }

    }

    private class MoveTrackingGridMouseAdapter extends GridMouseAdapter {

        public MoveTrackingGridMouseAdapter(GridPanel gridPanel, GridMouseListener listener) {
            super(gridPanel, listener);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            Point2D point = gridPanel.getRelativePoint(e.getPoint());
            int x = (int) point.getX();
            int y = (int) point.getY();
            Token newValue = null;
            for (Entry<Token, Rectangle> entry : areas.entrySet()) {
                if (entry.getValue().contains(x, y)) {
                    newValue = entry.getKey();
                    break;
                }
            }
            if (newValue != selected) {
                selected = newValue;
                gridPanel.repaint();
            }
        }

    }

    @Override
    public void onShow() {
        super.onShow();
        //TODO extract listener from this
        attachMouseInputListener(new MoveTrackingGridMouseAdapter(gridPanel, this));
    }

    @Override
    public void tileEntered(MouseEvent e, Position p) {
    }

    @Override
    public void tileExited(MouseEvent e, Position p) {

    }

    @Override
    public void mouseClicked(MouseEvent e, Position p) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (selected != null) {
                gc.getConnection().send(getAction().select(selected));
                e.consume();
            }
        }
    }


}
