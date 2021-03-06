/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */

package com.sikulix.guide;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Target;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

interface TrackerListener {
  void patternAnchored();
}

public class Tracker extends Thread {

  private static SXLog log = SX.getSXLog("SX.GUIDE.TRACKER");

  Guide guide;
  Target pattern;
  Element match;
  Element screen;
  String image_filename;
  Target centerPattern;
  boolean initialFound = false;
  ArrayList<Visual> components = new ArrayList<Visual>();
  ArrayList<Point> offsets = new ArrayList<Point>();
  SxAnchor anchor;
  TrackerListener listener;
  boolean running;

  public Tracker(Target pattern) {
    init(pattern, null);
  }

  public Tracker(Guide guide, Target pattern, Element match) {
    init(pattern, match);
    this.guide = guide;
  }

  private void init (Target pattern, Element match) {
    screen = Do.on();
    BufferedImage image;
    BufferedImage center;
    this.pattern = pattern;
    image = pattern.get();
    int w = image.getWidth();
    int h = image.getHeight();
    center = image.getSubimage(w / 4, h / 4, w / 2, h / 2);
    centerPattern = new Target(center);
    this.match = match;
  }

  public void setAnchor(Visual component) {
    Point loc = component.getLocation();
    //Point offset = new Point(loc.x - match.x, loc.y - match.y);
    Point offset = new Point(0, 0);//loc.x - match.x, loc.y - match.y);
    offsets.add(offset);
    components.add(component);
    anchor = (SxAnchor) component;
  }

  @Override
  public void run() {
    running = true;
    initialFound = true;
    match = null;
    // Looking for the target for the first time
    log.trace("Looking for the target for the first time");
    boolean hasMatch = false;
    while (running && !hasMatch) {
      hasMatch = screen.exists(pattern, 0.5);
    }
    // this means the tracker has been stopped before the pattern is found
    if (match == null) {
      return;
    }
    log.trace("Pattern is found for the first time");
    //<editor-fold defaultstate="collapsed" desc="TODO not used currently">
    //      if (true){
    //        Rectangle bounds = match.getRect();
    //        anchor.found(bounds);
    //      }else{
    //         // animate the initial movement to the anchor position
    //
    //         // uncomment this for popup demo
    //         anchor.moveTo(new Point(match.x, match.y), new AnimationListener(){
    //            public void animationCompleted(){
    //               anchor.anchored();
    //               if (listener != null){
    //                  listener.patternAnchored();
    //               }
    //            }
    //         });
    //      }
    //

    //</editor-fold>

    match = screen.getLastMatch();
    Rectangle bounds = match.getRectangle();
    anchor.found(bounds);

    while (running) {
      if (match != null && isPatternStillThereInTheSameLocation()) {
        //Debug.log("[Tracker] Pattern is seen in the same location.");
        continue;
      }
      // try for at least 1.0 sec. to have a better chance of finding the
      // new position of the pattern.
      // the first attempt often fails because the target is only a few
      // pixels away when the screen capture is made and it is still
      // due to occlusion by foreground annotations
      // however, it would mean it takes at least 1.0 sec to realize
      // the pattern has disappeared and the referencing annotations should
      // be hidden
      hasMatch = screen.exists(pattern, 1.0);
      Element newMatch = null;
      if (!hasMatch) {
        log.trace("Pattern is not found on the screen");
        //anchor.setOpacity(0.0f);
        //not_found_counter += 1;
        //if (not_found_counter > 2){
        anchor.addFadeoutAnimation();
        anchor.startAnimation();
        // not_found_counter = 0;
        //}
      } else {
        newMatch = screen.getLastMatch();
        log.trace("Pattern is found in a new location: %s", newMatch);
        // make it visible
        anchor.addFadeinAnimation();
        anchor.startAnimation();
//            anchor.setVisible(true);
//            // if the match is in a different location
//            if (match.x != newMatch.x || match.y != newMatch.y){
        //            for (int i=0; i < components.size(); ++i){
        // comp  = components.get(i);
        //Point offset = offsets.get(0);
//               int dest_x = newMatch.x + offset.x;
//               int dest_y = newMatch.y + offset.y;
        int dest_x = newMatch.x + newMatch.w / 2;
        int dest_y = newMatch.y + newMatch.h / 2;
        // comp.setEmphasisAnimation(comp.createMoveAnimator(dest_x, dest_y));
        //comp.startAnimation();
        log.trace("Pattern is moving to: (%d,%d)",dest_x, dest_y);
        anchor.moveTo(new Point(dest_x, dest_y));
      }
      match = newMatch;
    }
    //            if (!initialFound){
    //            Debug.log("[Tracker] Pattern has disappeared after initial find");
    //
    //
    //
    //            for (Visual comp : components){
    //               comp.setVisible(false);
    //            }
    //            guide.repaint();
    //         }
  }

  public void stopTracking() {
    running = false;
  }

  public boolean isAlreadyTracking(Target pattern, Element match) {
    //TODO isAlreadyTracking
    try {
      boolean sameMatch = this.match == match;
      boolean sameBufferedImage = this.pattern.get() == pattern.get();
      boolean sameFilename = false;
//      boolean sameFilename = (this.pattern.getFilename() != null &&
//              (this.pattern.getFilename().compareTo(pattern.getFilename()) == 0));
      return sameMatch || sameBufferedImage || sameFilename;
    } catch (Exception e) {
      return false;
    }
  }

  boolean isAnimationStillRunning() {
    for (Visual comp : components) {
      if (comp instanceof SxAnchor) {

        if (comp.animationRunning)
          return true;
        ;
      }
    }
    return false;
  }

  boolean isPatternStillThereInTheSameLocation() {
    try {
      sleep(1000);
    } catch (InterruptedException e) {
    }
    Element center = new Element(match);
    //<editor-fold defaultstate="collapsed" desc="TODO Pattern with BufferedImage">
    /* center.x += center.w/4-2;
     * center.y += center.h/4-2;
     * center.w = center.w/2+4;
     * center.h = center.h/2+4;
     */
    //</editor-fold>
    if (!center.exists(centerPattern, 0)) {
      log.trace("Pattern is not seen in the same location.");
      return false;
    }
    return true;
  }
}
