/*
 * ******************************
 * Name:           Jinke He
 * University ID:    201219022
 * Departmental ID:  x6jh
 * ******************************
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.controls.ActionListener;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.text.DecimalFormat;
import java.util.ArrayList;
import static mygame.Arkanoid.initSettings;

public class Arkanoid extends SimpleApplication implements ScreenController {

    // define basic elements
    private Nifty nifty;
    private static Arkanoid app;
    private static AppSettings mySettings;
    
    // define constant variables
    private final float paddleSpeed = 7f;
    private final float initBallSpeed = 5f;
    private final float ballScale = 0.15f;
    private final float ballRadius = 0.15f;
    private final float ballFallenPauseTime = 3f; // after a ball dies
    private final float boomRadius = 2f;
    private final float idleTimeLimit = 3.5f;
    private final float propsWaitingTime = 5f;
    private final float blastingTimeLimit = 1.5f;
    private final int numOfLevels = 7;
    private final Vector3f wallInitLocation = new Vector3f(-1.5f, 1.5f, 0);
    private final Vector3f paddleInitLocation = new Vector3f(0, -3.6f, 0);
    private final Vector3f ballInitLocation = paddleInitLocation.add(0, 0.5f, 0);
    private final Vector3f propsVelocity = new Vector3f(1.5f, -1.5f, 0);
    private final DecimalFormat df = new DecimalFormat("#.#");
    
    // define audio nodes
    private AudioNode hitBall;
    private AudioNode touchBoundary;
    private AudioNode ballFallen;
    private AudioNode background;
    private AudioNode win;
    private AudioNode lose;
    private AudioNode levelUp;
    private AudioNode explosion;
    
    // define game statistics (texts)
    private BitmapText scoreText;
    private BitmapText speedText;
    private BitmapText messageText;
    private BitmapText boomText;
    private BitmapText healthText;
    
    // define vectors
    private Vector3f velocity = new Vector3f(0, initBallSpeed, 0); // the velocity of the ball
    private Vector3f direction; // the direction of the velocity
    
    // define the array list of target balls
    private ArrayList<Geometry> targetBalls;
    
    // define game pauses
    private boolean ballFallenPause = false; // the ball dies
    private boolean playerPause = false; // the player chooses to pause
    private boolean levelUpPause = false; // the game goes into next level and the game will pause for a few seconds
    
    // define game information
    private int scores = 0; // the scores earned by the player
    private int health = 4; // the health of the player
    private int ballsRemain; // the number of balls remained
    private int level = 1; // the level of the game, starting from level 1
    private boolean hasBoom = false; // whether the player has a boom
    private boolean directionSelected = false; // whether the direction has been selected
    private boolean isBlasting = false;
    private boolean inGame = false;
    private float angular = 0;
    private String message = "";
    
    // define time counters
    private float timer1 = 0;
    private float propsTimer = 0;
    private float blastingTime = 0;
    private float idleTimer = 0;
    
    // define geometries and spatials and shapes
    private Geometry props;
    private Geometry directionArrow;
    private Geometry ball;
    private Geometry propsWall;
    private Spatial wholeWall;
    private Spatial topWall;
    private Spatial leftWall;
    private Spatial rightWall;
    private Spatial paddle;
    private ParticleEmitter explosionEffect;
    private ParticleEmitter ballFire;
    private Arrow arrow;
    private Sphere ballSphere = new Sphere(100, 100, ballRadius);
    private Material ballMaterial;

    // initialize the settings of the game
    public static void initSettings() {
        mySettings = new AppSettings(false);
        mySettings.setHeight(1024); // by default, the height is set to 1024
        mySettings.setWidth(1280); // by default, the width is set to 1280
        mySettings.setMinHeight(768);
        mySettings.setMinWidth(1024);
        mySettings.setTitle("Arkanoid - COMP222 Assignment 1 - Jinke He");
        mySettings.setSettingsDialogImage("/com/jme3/app/Monkey.png");
    }

    // the main function -> start the game
    public static void main(String[] args) {
        initSettings();
        app = new Arkanoid();
        app.setSettings(mySettings);
        app.start();
    }

    // initialize the game
    public void simpleInitApp() {

        // initialize Nifty GUI
        initNifty();

        // initialie the key mappings
        initKey();

        // initialize the views of the game
        initView();

        // initialize the sounds
        initSound();

        // initialize the light
        initLight();

        // initialize the wall
        initWall();

        // initialize the paddle
        initPaddle();

        // initialize the fire at the bottom of the field
        initBoomFire();

        // initialize the location and the velocity of the ball
        initBall();

        // initialize the texts
        initText();

        // welcome
        System.out.println("Welcome to the game Arkanoid - Jinke He.");
        System.out.println("You can press Enter or Space or click the button to start.");
    }

    // initialize nifty GUI
    private void initNifty() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, viewPort);
        nifty = niftyDisplay.getNifty();
        nifty.fromXml("Interface/ui.xml", "start", this);
        nifty.gotoScreen("start");
        guiViewPort.addProcessor(niftyDisplay);
    }

    // initialize the key mappings
    private void initKey() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Boom", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Confirm", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Pass", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("Pause", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("Start", new KeyTrigger(KeyInput.KEY_SPACE), new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(actionListener, "Pass");
        inputManager.addListener(analogListener, "Left");
        inputManager.addListener(analogListener, "Right");
        inputManager.addListener(actionListener, "Boom");
        inputManager.addListener(actionListener, "Start");
        inputManager.addListener(actionListener, "Pause");
        inputManager.addListener(actionListener, "Confirm");
    }
    /* initialize the action listener of the game
     * 
     * 1. Press Enter or Space to start or quit the game 
     * 2. Press Space to select the initial direction of the ball
     * 3. Press 'x' to release the boom if the player has one
     * 4. Press 'z' to pass the current level
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!inGame) { // if the game hasn't started or has finished
                if (name.equals("Start") && !isPressed && nifty.getCurrentScreen() == nifty.getScreen("start")) {
                    next(); // start the game
                } else if (name.equals("Start") && !isPressed && nifty.getCurrentScreen() == nifty.getScreen("keymap")) {
                    startGame();
                } else if (name.equals("Start") && !isPressed && nifty.getCurrentScreen() == nifty.getScreen("win") || nifty.getCurrentScreen() == nifty.getScreen("lose")) {
                    quitGame(); // end the game
                }
            } else if (!levelUpPause) { // if the game is in the screen of levelUP
                if (name.equals("Pause") && !isPressed) {
                    if (playerPause == true) {
                        playerPause = false;
                        message = "Resumed.";
                    } else {
                        playerPause = true;
                        message = "Paused. Press 'P' to resume.";
                    }
                } else if (name.equals("Pass") && !isPressed) { // pass the current level
                    directionSelected = true;
                    ballsRemain = 0;
                    for (Geometry ball : targetBalls) {
                        ball.setUserData("isRemoved", true);
                        rootNode.detachChild(ball);
                    }
                    if (directionArrow != null) {
                        rootNode.detachChild(directionArrow);
                        directionArrow = null;
                    }
                    playerPause = false;
                } else if (playerPause == false) {
                    if (directionSelected == false) { // conform the selection of initial direction
                        if (name.equals("Confirm")) {
                            directionSelected = true;
                            if (ballFallenPause == true) {
                                velocity = direction.mult(initBallSpeed);
                            } else {
                                velocity = direction.mult(velocity.length());
                            }
                            if (directionArrow != null) {
                                rootNode.detachChild(directionArrow);
                                directionArrow = null;
                            }
                        }
                    } else { // if the player is playing the game
                        if (name.equals("Boom") && hasBoom && !isPressed) {
                            initExplosion();
                            int count = 0;
                            for (Geometry targetBall : targetBalls) {
                                if (ball != null) {
                                    if ((Boolean) targetBall.getUserData("isRemoved") == false && (targetBall.getLocalTranslation().subtract(ball.getLocalTranslation())).length() < boomRadius) {
                                        scores += 1;
                                        rootNode.detachChild(targetBall);
                                        targetBall.setUserData("isRemoved", true);
                                        count++;
                                    }
                                }
                            }
                            scores += count;
                            ballsRemain -= count;
                           velocity = velocity.add(velocity.normalize().mult(0.15f * count)); // speed up
                            boomText.setText("Your boom killed " + count + " balls.");
                            hasBoom = false;
                            ballMaterial.setColor("Diffuse", ColorRGBA.Red);
                            ballMaterial.setColor("Specular", ColorRGBA.Red);
                        }
                    }
                }
            }

        }
    };
    /*
     * initialize the analog listener
     * 
     * 1. Press Left or Up to move the direction arrow counterclockwise
     * 2. Press Right or Down to move the directio arrow clockwise
     */
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if (inGame && playerPause == false && levelUpPause == false) { // if the game is not paused
                if (directionSelected == false) { // if the direction is not selected, the player can rotate the arrow to select the velocity
                    if (name.equals("Left") && !directionSelected) {
                        angular -= tpf * 2f;
                    } else if (name.equals("Right") && !directionSelected) {
                        angular += tpf * 2f;
                    }
                } else { // if the direction is already selected, the player can move the paddle to left or right
                    if (name.equals("Left") && (Boolean) paddle.getUserData("reachLeft") == false) {
                        paddle.setUserData("reachRight", false);
                        paddle.move(-paddleSpeed * tpf, 0, 0);
                    } else if (name.equals("Right") && (Boolean) paddle.getUserData("reachRight") == false) {
                        paddle.setUserData("reachLeft", false);
                        paddle.move(paddleSpeed * tpf, 0, 0);
                    }
                }
            }
        }
    };

    // initialize the camera and views of the game
    private void initView() {
        flyCam.setEnabled(false);
        setDisplayFps(true);
        setDisplayStatView(false);
        setPauseOnLostFocus(true);
        rootNode.move(1.7f, 0, 0);
    }

    // initialize the light of the game
    private void initLight() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-2f, 0f, -2f)));
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
    }

    // initialize the wall 
    private void initWall() {
        wholeWall = assetManager.loadModel("Models/new_new_wall.j3o"); // move the wall the right position
        wholeWall.rotate(0, -FastMath.PI * 0.5f, -FastMath.PI * 0.5f);
        wholeWall.setLocalTranslation(wallInitLocation);
        wholeWall.scale(0.44f, 0.62f, 0.62f);
        rootNode.attachChild(wholeWall);
        leftWall = ((Node) wholeWall).getChild("left");
        rightWall = ((Node) wholeWall).getChild("right");
        topWall = ((Node) wholeWall).getChild("top");
        Box box = new Box(2.5f, 2.5f, 1.0f); // the props wall is a piece of the wall
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Orange);
        mat.setBoolean("UseMaterialColors", true);
        mat.setTexture("DiffuseMap", assetManager.loadTexture("Blender/2.4x/WoodCrate_lighter - Height Map.png"));
        propsWall = new Geometry("propsWall", box); // initialize the props wall
        propsWall.setShadowMode(ShadowMode.Off);
        propsWall.setMaterial(mat);
        propsWall.setLocalTranslation(-2.8f, 3.78f, 0);
        propsWall.scale(0.19f, 0.05f, 0.1f);
        rootNode.attachChild(propsWall);
    }

    // initialize the direction arrow, which is used to select the direction of the ball's velocity
    private void initArrow() {
        directionSelected = false;
        direction = Vector3f.UNIT_X;
        arrow = new Arrow(direction);
        arrow.setLineWidth(5);
        directionArrow = new Geometry("arrow", arrow);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        directionArrow.setMaterial(mat);
        directionArrow.setLocalTranslation(ball.getLocalTranslation());
        rootNode.attachChild(directionArrow);
    }

    // initialize the fire attached to the ball
    private void initBoomFire() {
        ParticleEmitter boomFire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material fireMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        fireMat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        boomFire.setMaterial(fireMat);
        boomFire.setImagesX(9);
        boomFire.setImagesY(4);
        boomFire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));
        boomFire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f));
        boomFire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 4f, 0));
        boomFire.setStartSize(0.55f);
        boomFire.setEndSize(0.1f);
        boomFire.setGravity(0, 0, 0);
        boomFire.setLowLife(0.2f);
        boomFire.setHighLife(0.4f);
        boomFire.getParticleInfluencer().setVelocityVariation(0.3f);
        boomFire.setLocalTranslation(paddle.getLocalTranslation().subtract(0, 0.6f, 0));
        rootNode.attachChild(boomFire.move(0, -0.3f, 0)); // add fire to the bottom of the screen
        rootNode.attachChild(boomFire.clone().move(2.5f, 0, 0));
        rootNode.attachChild(boomFire.clone().move(1.8f, 0, 0));
        rootNode.attachChild(boomFire.clone().move(-0.9f, 0, 0));
        rootNode.attachChild(boomFire.clone().move(-2.7f, 0, 0));
        rootNode.attachChild(boomFire.clone().move(-1.9f, 0, 0));
        rootNode.attachChild(boomFire.clone().move(0.7f, 0, 0));
    }

    // initialize the fire of the ball
    private ParticleEmitter initBallFire() {
        ParticleEmitter fireEffect = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 15);
        Material fireMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        fireMat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/shockwave.png"));
        fireEffect.setMaterial(fireMat);
        fireEffect.setImagesX(1);
        fireEffect.setImagesY(1);
        fireEffect.setEndColor(ColorRGBA.Orange);
        fireEffect.setStartColor(ColorRGBA.Yellow);
        fireEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2f, 0));
        fireEffect.setStartSize(0.2f);
        fireEffect.setEndSize(0.1f);
        fireEffect.setGravity(0, 0, 0);
        fireEffect.setLowLife(0.1f);
        fireEffect.setHighLife(0.2f);
        fireEffect.getParticleInfluencer().setVelocityVariation(0.2f);
        return fireEffect;
    }

    // intialize the paddle
    private void initPaddle() {
        paddle = assetManager.loadModel("Models/paddle.j3o");
        paddle.setLocalTranslation(paddleInitLocation);
        paddle.scale(ballScale);
        rootNode.attachChild(paddle);
        paddle.setUserData("reachRight", false);
        paddle.setUserData("reachLeft", false);
        paddle.scale(2, 1.5f, 1);
    }

    // initialize the various texts
    private void initText() {
        int x = 20; // the proper position under resolution of 1024x768
        int y = 400;
        float xScale = mySettings.getWidth() / 1024; // adjust it to the current resolution
        float yScale = mySettings.getHeight() / 768;
        x *= xScale;
        y *= yScale; // scale the texts to suit the current resolution
        float textScale = 0.00069444f * mySettings.getWidth() + 0.8888f; // scale = 0.00069444f * width + 0.8888f (according to my calculation)

        // the text to show the score achieved by the player
        scoreText = new BitmapText(guiFont, false);
        scoreText.setColor(ColorRGBA.Black);
        scoreText.setSize(guiFont.getCharSet().getRenderedSize() * textScale);
        scoreText.setLocalTranslation(x, scoreText.getLineHeight() + y, 0);
        guiNode.attachChild(scoreText);

        // the text to show the current speed of the ball
        speedText = new BitmapText(guiFont, false);
        speedText.setColor(ColorRGBA.Black);
        speedText.setSize(guiFont.getCharSet().getRenderedSize() * textScale);
        speedText.setLocalTranslation(x, speedText.getLineHeight() + y + 100, 0);
        guiNode.attachChild(speedText);

        // the text to show the health of the player
        healthText = new BitmapText(guiFont, false);
        healthText.setColor(ColorRGBA.Black);
        healthText.setSize(guiFont.getCharSet().getRenderedSize() * textScale);
        healthText.setLocalTranslation(x, healthText.getLineHeight() + y + 200, 0);
        guiNode.attachChild(healthText);

        // the text to notify the player that he/she has a boom
        boomText = new BitmapText(guiFont, false);
        boomText.setColor(ColorRGBA.Red);
        boomText.setSize(guiFont.getCharSet().getRenderedSize() * textScale);
        boomText.setLocalTranslation(x, boomText.getLineHeight() + y + 250, 0);
        guiNode.attachChild(boomText);

        // the text to show the system message
        messageText = new BitmapText(guiFont, false);
        messageText.setColor(ColorRGBA.Black);
        messageText.setSize(guiFont.getCharSet().getRenderedSize() * textScale);
        messageText.setLocalTranslation(x, messageText.getLineHeight() + y + 300, 0);
        guiNode.attachChild(messageText);
    }

    // initlize the sounds
    private void initSound() {

        // when the ball hits a target ball
        hitBall = new AudioNode(assetManager, "Sounds/touchOne.wav", false);
        hitBall.setPositional(false);
        hitBall.setLooping(false);
        hitBall.setVolume(1);

        // when the ball touches the boundary
        touchBoundary = new AudioNode(assetManager, "Sounds/Jump_01.wav", false);
        touchBoundary.setPositional(false);
        touchBoundary.setLooping(false);
        touchBoundary.setVolume(1);

        // when the ball is dead
        ballFallen = new AudioNode(assetManager, "Sounds/fallDown.wav", false);
        ballFallen.setPositional(false);
        ballFallen.setLooping(false);
        ballFallen.setVolume(1);

        // the background music
        background = new AudioNode(assetManager, "Sounds/01.wav", false);
        background.setPositional(false);
        background.setLooping(true);
        background.setVolume(0.5f);
        background.play();

        // the sound of explosion
        explosion = new AudioNode(assetManager, "Sounds/explosion.wav", false);
        explosion.setPositional(false);
        explosion.setLooping(false);
        explosion.setVolume(2);

        // when the player passes a level
        levelUp = new AudioNode(assetManager, "Sounds/levelUp.wav", false);
        levelUp.setPositional(false);
        levelUp.setLooping(false);
        levelUp.setVolume(1);

        // when the player wins
        win = new AudioNode(assetManager, "Sounds/win.wav", false);
        win.setPositional(false);
        win.setLooping(true);
        win.setVolume(1);

        // when the player loses
        lose = new AudioNode(assetManager, "Sounds/lose.wav", false);
        lose.setPositional(false);
        lose.setLooping(false);
        lose.setVolume(1);
    }

    // initialize the location and the velocity of the moving ball
    private void initBall() {
        ball = new Geometry("ball", ballSphere);
        ballMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        ballMaterial.setBoolean("UseMaterialColors", true);
        ballMaterial.setColor("Diffuse", ColorRGBA.Red);
        ballMaterial.setColor("Specular", ColorRGBA.Red);
        ball.setMaterial(ballMaterial);
        ball.setLocalTranslation(ballInitLocation);
        ballFire = initBallFire();
        ballFire.setLocalTranslation(ball.getLocalTranslation());
        rootNode.attachChild(ball);
        rootNode.attachChild(ballFire);
    }

    // a timer function
    private void timer(float tpf) {
        if (inGame) {
            // when the sound of level up finishes, the background music will continue
            if (levelUpPause == true && levelUp.getStatus() == AudioSource.Status.Stopped) {
                levelUpPause = false;
                nifty.gotoScreen("playing");
                background.play();
            }
            if (isBlasting == true) { // the explosion will last for 1.5s
                if (blastingTime < blastingTimeLimit) {
                    blastingTime += tpf;
                } else {
                    blastingTime = 0;
                    rootNode.detachChild(explosionEffect);
                    isBlasting = false;
                }
            }
        }
    }

    // initialize the explosion 
    private void initExplosion() {
        explosionEffect = new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 60);
        Material debrisMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        debrisMat.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/shockwave.png"));
        explosionEffect.setMaterial(debrisMat);
        explosionEffect.setImagesX(4);
        explosionEffect.setImagesY(4); // 3x3 texture animation
        explosionEffect.setRotateSpeed(10);
        explosionEffect.setSelectRandomImage(true);
        explosionEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0, 0));
        explosionEffect.setStartColor(ColorRGBA.Yellow);
        explosionEffect.getParticleInfluencer().setVelocityVariation(.40f);
        explosionEffect.setGravity(0, 0, 0);
        explosionEffect.setLocalScale(2);
        explosionEffect.setLocalTranslation(ball.getLocalTranslation());
        rootNode.attachChild(explosionEffect);
        explosionEffect.emitAllParticles();
        isBlasting = true;
        explosion.playInstance();
    }

    /* 
     * release a props from the props wall
     * 
     * There are three kinds of props:
     * 1. a boom - the player can use it by pressing 'x'
     * 2. elongate the paddle
     * 3. an extra health
     * 
     * each props has a unique color
     * each props is kind of gift to the player 
     */
    private void initProps() {
        propsTimer = 0;
        rootNode.detachChild(propsWall);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        props = new Geometry("ball", ballSphere);
        props.setMaterial(mat);
        props.setLocalTranslation(propsWall.getLocalTranslation().add(0, -1, 0));
        rootNode.attachChild(props);
        // randomly release a props
        double choice = Math.random() * 3;
        if (choice > 2) {
            // a boom
            props.setUserData("powerType", "Boom");
            mat.setColor("Diffuse", ColorRGBA.Yellow);
            mat.setColor("Specular", ColorRGBA.Yellow);
        } else if (choice > 1) {
            // elongate the paddle
            props.setUserData("powerType", "Enlongate");
            mat.setColor("Diffuse", ColorRGBA.Orange);
            mat.setColor("Specular", ColorRGBA.Orange);
        } else {
            // an extra health
            props.setUserData("powerType", "HealthUp");
            mat.setColor("Diffuse", ColorRGBA.Red);
            mat.setColor("Specular", ColorRGBA.Red);
        }
    }

    // if the paddle reaches the boundary of the wall, the paddle can't move anymore
    private void collidePaddleAndWall() {
        CollisionResults resultTabAndWall = new CollisionResults();
        wholeWall.collideWith(paddle.getWorldBound(), resultTabAndWall);
        if (resultTabAndWall.size() != 0) {
            if (paddle.getLocalTranslation().x < 0) {
                paddle.setUserData("reachLeft", true);
            } else if (paddle.getLocalTranslation().x > 0) {
                paddle.setUserData("reachRight", true);
            }
        }
    }

    // the ball hits another ball (the target ball or the props)
    private void collideBallAndBall(float tpf) {
        CollisionResults ballAndBall = new CollisionResults();
        if (props != null) { // if there is a props 
            props.move(propsVelocity.mult(tpf));
            CollisionResults resultPropsAndWall = new CollisionResults();
            wholeWall.collideWith(props.getWorldBound(), resultPropsAndWall);
            if (resultPropsAndWall.size() > 0) { // if the props reaches the boundary of the wall
                rootNode.detachChild(props); // remove it 
                rootNode.attachChild(propsWall);
                props = null;
            } else { // check whether it will collide with the red ball
                props.collideWith(ball.getWorldBound(), ballAndBall);
            }
        }
        for (Geometry targetBall : targetBalls) { // for each target ball, check whether it will collide with the red ball
            if (!(Boolean) targetBall.getUserData("isRemoved")) {
                targetBall.collideWith(ball.getWorldBound(), ballAndBall);
            }
        }
        // the red ball hits the props or a target ball
        if (ballAndBall.size() > 0) {
            idleTimer = 0;
            if (ballAndBall.getClosestCollision().getGeometry() == props) { // the ball hits the props(moving)
                if ("Boom".equals((String) props.getUserData("powerType"))) {
                    hasBoom = true;
                    message = "You've got a nuclear weapon!";
                    ballMaterial.setColor("Diffuse", ColorRGBA.Yellow); // change the ball's color to Yellow
                    ballMaterial.setColor("Specular", ColorRGBA.Yellow);
                } else if ("Enlongate".equals((String) props.getUserData("powerType"))) {
                    message = "Your tab has been enlongated!";
                    paddle.scale(1.23f, 1, 1);
                } else if ("HealthUp".equals((String) props.getUserData("powerType"))) {
                    health++;
                    message = "You've got an extra HP.";
                }
                getNewVelocity(props);
                scores += 5;
                rootNode.detachChild(props);
                rootNode.attachChild(propsWall);
                props = null;
            } else { // the ball hits a target ball (static)
                Geometry targetBall = ballAndBall.getClosestCollision().getGeometry();
                getNewVelocity(targetBall);
                targetBall.setUserData("isRemoved", true);
                scores += 1;
                ballsRemain--;
                message = "You removed one target ball.";
                rootNode.detachChild(targetBall);
            }
            hitBall.playInstance();
        }
        if (propsTimer > propsWaitingTime) { // generate a new props every 5 seconds since last props disappered
            initProps();
        } else if (props == null) {
            propsTimer += tpf;
        }
    }

    /* 
     * get the new velocity of the ball
     * 
     * ball2 is the ball that is hit by the red ball and velocity2 is its velocity
     */
    private void getNewVelocity(Geometry ball2) { // ball2 is the target and ball is the red ball
        Vector3f normalDirection = ball.getLocalTranslation().subtract(ball2.getLocalTranslation()).normalize();
        float normalVelocity1 = velocity.dot(normalDirection);
        velocity.subtractLocal(normalDirection.mult(2 * normalVelocity1));
        velocity = velocity.add(velocity.normalize().mult(0.15f)); // speed up
    }

    // the ball collides with the wall
    private void collideBallAndWall() {
        CollisionResults resultLeft = new CollisionResults();
        leftWall.collideWith(ball.getWorldBound(), resultLeft);
        CollisionResults resultRight = new CollisionResults();
        rightWall.collideWith(ball.getWorldBound(), resultRight);
        CollisionResults resultTop = new CollisionResults();
        topWall.collideWith(ball.getWorldBound(), resultTop);
        propsWall.collideWith(ball.getWorldBound(), resultTop);
        if (resultTop.size() != 0 || ball.getLocalTranslation().y > propsWall.getLocalTranslation().y) {
            velocity.y = -1 * FastMath.abs(velocity.y);
            touchBoundary.playInstance();
        }
        if (resultLeft.size() != 0) {
            velocity.x = FastMath.abs(velocity.x);
            touchBoundary.playInstance();
        }
        if (resultRight.size() != 0) {
            velocity.x = -1 * FastMath.abs(velocity.x);
            touchBoundary.playInstance();
        }
    }

    // the ball collides with the paddle
    private void collideBallAndPaddle() {
        CollisionResults result = new CollisionResults();
        paddle.collideWith(ball.getWorldBound(), result);
        if (result.size() != 0 && velocity.y < 0) {
            velocity.y = FastMath.abs(velocity.y);
            touchBoundary.playInstance();
        }
    }

    // update the texts
    private void textUpdate() {
        scoreText.setText("Your Score: " + scores);
        speedText.setText("Speed: " + df.format(velocity.length()));
        healthText.setText("Health: " + health);
        if (hasBoom == true) {
            boomText.setText("Press 'X' to release the boom!");
        } else {
            boomText.setText("");
        }
        messageText.setText(message);
    }

    // check the position of the ball
    private void checkBallPosition() {
        if (ball.getLocalTranslation().y < paddle.getLocalTranslation().y - 0.2f) {
            idleTimer = 0;
            message = "You are dead!";
            ballFallen.playInstance();
            health --;
            velocity.subtractLocal(velocity.normalize().mult((velocity.length() - initBallSpeed)/1.5f)); // slow down the ball's velocity            health--;
            if (health != 0) { // if the player's health is not 0, the ball revives after a delay
                paddleAndBallReset();
            } else { // if the player has no health, the player loses
                lose();
            }
        }
    }

    // check the direction of the ball's velocity to avoid the ball moves horizontally or vertically
    private void checkBallDirection() {
        if (Math.abs(velocity.x / velocity.y) > 10 || Math.abs(velocity.y / velocity.x) > 15) { // if the ball is moving nearly horizontally or vertically
            if (idleTimer > idleTimeLimit) { // and the ball hasn't hit any ball for an amount of time
                initArrow(); // the player can choose the direction of the ball's velocity again
                idleTimer = 0;
            }
        } else { // if the angle of the velocity is normal, reset the timer
            idleTimer = 0;
        }
    }

    // the simple update method
    @Override
    public void simpleUpdate(float tpf) {
        timer(tpf);
        if (inGame && !levelUpPause) {
            // change the contents of texts
            textUpdate();
            if (directionSelected == false) {
                direction.x = FastMath.sin(angular);
                direction.y = FastMath.cos(angular);
                arrow.setArrowExtent(direction);
            } else {
                idleTimer += tpf;
                collidePaddleAndWall();
                if (!ballFallenPause && !playerPause) {
                    // the ball can move
                    ball.move(velocity.mult(tpf));
                    ballFire.setLocalTranslation(ball.getLocalTranslation());
                    ballFire.getParticleInfluencer().setInitialVelocity(velocity.mult(-0.5f));
                    // handle collision between ball and balls
                    collideBallAndBall(tpf);
                    // handle collision between ball and walls
                    collideBallAndWall();
                    // handle collision between ball and tab
                    collideBallAndPaddle();
                    // the ball has gone beyond the boundary
                    checkBallPosition();
                    // check whether the ball is moving horizontally or vertically
                    checkBallDirection();
                } else if (ballFallenPause) {
                    timer1 += tpf;
                    if (timer1 > ballFallenPauseTime) {
                        ballFallenPause = false;
                        timer1 = 0;
                    }
                }
                if (ballsRemain > 0.75 * targetBalls.size()) {
                    viewPort.setBackgroundColor(new ColorRGBA(0.74f, 0.74f, 0.74f, 1.0f));
                } else if (ballsRemain > 0.5 * targetBalls.size()) {
                    viewPort.setBackgroundColor(new ColorRGBA(0.76f, 0.76f, 0.76f, 1.0f));
                } else if (ballsRemain > 0.25 * targetBalls.size()) {
                    viewPort.setBackgroundColor(new ColorRGBA(0.79f, 0.79f, 0.79f, 1.0f));
                } else {
                    viewPort.setBackgroundColor(new ColorRGBA(0.82f, 0.82f, 0.82f, 1f));
                }
                // check whether the user has passed this level
                if (ballsRemain == 0 && isBlasting == false) {
                    for (int i = 0; i <= targetBalls.size() - 1; i++) {
                        rootNode.detachChild(targetBalls.get(i));
                    }
                    level++;
                    targetBalls.clear();
                    loadLevel(level);
                    message = "Level " + level;
                }
            }
        } else { // if the game is over or hasn't started, hide all the texts
            scoreText.setText("");
            healthText.setText("");
            speedText.setText("");
            messageText.setText("");
            boomText.setText("");
        }
    }

    // reset the ball and paddle to their initial state
    private void paddleAndBallReset() {
        paddle.setLocalTranslation(paddleInitLocation);
        ball.setLocalTranslation(ballInitLocation);
        paddle.setUserData("reachLeft", false);
        paddle.setUserData("reachRight", false);
        initArrow();
    }

    // reset the game and load next level
    private void loadLevel(int level) {
        if (health < 5){
            health ++;
        }
        if (props != null) {
            rootNode.detachChild(props);
            props = null;
            rootNode.attachChild(propsWall);
        }
        propsTimer = 0;
        idleTimer = 0;
        ballFire.setLocalTranslation(new Vector3f(100, 0, 0));
        if (level <= numOfLevels) {
            levelUpPause = true;
            nifty.getScreen("levelUp").findElementByName("level").getRenderer(TextRenderer.class).setText("Level " + level);
            nifty.gotoScreen("levelUp");
            targetBalls = new ArrayList<Geometry>();
            background.pause();
            levelUp.play();
            angular = 0;
            switch (level) {
                case 1:
                    levelOne();
                    break;
                case 2:
                    levelTwo();
                    break;
                case 3:
                    levelThree();
                    break;
                case 4:
                    levelFour();
                    break;
                case 5:
                    levelFive();
                    break;
                case 6:
                    levelSix();
                    break;
                case 7:
                    levelSeven();
                    break;
            }
            viewPort.setBackgroundColor(new ColorRGBA(0.74f, 0.74f, 0.74f, 1.0f));
            ballsRemain = targetBalls.size();
            paddleAndBallReset();
        } else { // the player wins
            win();
        }
    }

    // Create a green ball and add it to the screen
    private void makeBall(float x, float y) {
        Material targetBallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        targetBallMat.setBoolean("UseMaterialColors", true);
        targetBallMat.setColor("Diffuse", ColorRGBA.Green);
        targetBallMat.setColor("Specular", ColorRGBA.Green);
        targetBallMat.setTransparent(true);
        ballSphere.setTextureMode(Sphere.TextureMode.Projected);
        Geometry temp = new Geometry("Ball", ballSphere);;
        temp.setMaterial(targetBallMat);
        temp.setLocalTranslation(x, y, 0);
        temp.setUserData("isRemoved", false);
        targetBalls.add(temp);
        rootNode.attachChild(temp);
    }

    // Level 1
    public void levelOne() {
        makeBall(-0.4f, 1.4f);
        makeBall(-1.4f, 1.4f);
        makeBall(-2.5f, 1.4f);
        makeBall(0.6f, 1.4f);
        makeBall(1.6f, 1.4f);
        makeBall(-0.4f, 2.4f);
        makeBall(-0.4f, 3.4f);
        makeBall(-0.4f, 0.4f);
        makeBall(-0.4f, -0.6f);
    }

    // level 2
    public void levelTwo() {
        makeBall(-0.4f, 0.3f);
        makeBall(-1.4f, 0.3f);
        makeBall(0.6f, 0.3f);
        makeBall(-1.07f, 1.3f);
        makeBall(-0.73f, 2.3f);
        makeBall(-0.07f, 2.3f);
        makeBall(0.26f, 1.3f);
        makeBall(-0.4f, 1.3f);
        makeBall(-0.4f, 2.3f);
        makeBall(-0.4f, 3.3f);
        makeBall(0.93f, -0.7f);
        makeBall(-1.73f, -0.7f);
        makeBall(-0.4f, -0.7f);
    }

    // Level 3
    public void levelThree() {
        makeBall(0.5f, 1);
        makeBall(1, 1);
        makeBall(1.5f, 1);
        makeBall(2, 1);
        makeBall(2.5f, 1);
        makeBall(-0.5f, 1);
        makeBall(-1, 1);
        makeBall(-1.5f, 1);
        makeBall(-2f, 1);
        makeBall(-2.5f, 1);
        makeBall(0.5f, 3);
        makeBall(1, 3);
        makeBall(1.5f, 3);
        makeBall(2, 3);
        makeBall(2.5f, 3);
        makeBall(-0.5f, 3);
        makeBall(-1f, 3);
        makeBall(-1.5f, 3);
        makeBall(-2f, 3);
        makeBall(-2.5f, 3);
        makeBall(0, 1);
        makeBall(0, 3);
    }

    // Level 4
    public void levelFour() {
        makeBall(0, 2);
        makeBall(-2.f, 2);
        makeBall(1.9f, 2);
        makeBall(-1.1f, 3);
        makeBall(0.9f, 3);
        makeBall(-1.1f, 1);
        makeBall(0.9f, 1);
        makeBall(-2.8f, 1);
        makeBall(-2.8f, 3);
        makeBall(2.6f, 1);
        makeBall(2.6f, 3);
        makeBall(0, 0);
    }

    // Level 5
    public void levelFive() {
        hasBoom = true; // give you a boom for free
        ballMaterial.setColor("Diffuse", ColorRGBA.Yellow);
        makeBall(0, 2);
        makeBall(0.3f, 2.5f);
        makeBall(0.6f, 3);
        makeBall(-0.3f, 2.5f);
        makeBall(-0.6f, 3);
        makeBall(0.9f, 2.5f);
        makeBall(-0.9f, 2.5f);
        makeBall(-1.2f, 2);
        makeBall(1.2f, 2);
        makeBall(1.5f, 2.5f);
        makeBall(-1.5f, 2.5f);
        makeBall(1.8f, 3);
        makeBall(-1.8f, 3);
        makeBall(2.1f, 2.5f);
        makeBall(-2.1f, 2.5f);
        makeBall(2.4f, 2);
        makeBall(-2.4f, 2);
        makeBall(0, 1);
        makeBall(0.6f, 1);
        makeBall(-0.6f, 1);
        makeBall(-1.2f, 1);
        makeBall(1.2f, 1);
        makeBall(1.8f, 1);
        makeBall(-1.8f, 1);
        makeBall(2.4f, 1);
        makeBall(-2.4f, 1);

    }

    // Level 6
    public void levelSix() {
        makeBall(0f, 1.6f);
        makeBall(-0.5f, 2.1f);
        makeBall(0.5f, 2.1f);
        makeBall(-1f, 2.6f);
        makeBall(1f, 2.6f);
        makeBall(-1.5f, 2.1f);
        makeBall(1.5f, 2.1f);
        makeBall(-1.5f, 1.6f);
        makeBall(1.5f, 1.6f);
        makeBall(-1.5f, 1.1f);
        makeBall(1.5f, 1.1f);
        makeBall(-1f, 0.6f);
        makeBall(1f, 0.6f);
        makeBall(-0.5f, 0.1f);
        makeBall(0.5f, 0.1f);
        makeBall(0, -0.4f);
    }
    
    public void levelSeven(){
        // make a circle
        float r = 2f;
        for (float i = 0; i <= r; i += r / 5) {
            if (i != 0 && i != r) {
                makeBall(i, (float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
                makeBall(-i, (float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
                makeBall(i, -(float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
                makeBall(-i, -(float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
            } else if (i == 0) {
                makeBall(0, r + 1.2f);
                makeBall(0, -r + 1.2f);
            } else if (i == r) {
                makeBall(r, 0 + 1.2f);
                makeBall(-r, 0 + 1.2f);
            }
        }
        // make it look like a circle
        float i = 19 * r / 20;
        makeBall(i, (float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
        makeBall(-i, (float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
        makeBall(i, -(float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
        makeBall(-i, -(float) Math.sqrt(Math.pow(r, 2) - Math.pow(i, 2)) + 1.2f);
    }

    // the player wins
    public void win(){
        inGame = false;
        background.pause();
        win.play();
        nifty.getScreen("win").findElementByName("score").getRenderer(TextRenderer.class).setText("Your Score: " + scores);
        nifty.gotoScreen("win");
    }
    
    // the player loses
    public void lose(){
        nifty.getScreen("lose").findElementByName("score").getRenderer(TextRenderer.class).setText("Your Score: " + scores);
        nifty.gotoScreen("lose");
        directionSelected = false;
        background.pause();
        inGame = false;
        lose.play();
    }
        
    /* Nifty GUI methods */
    // start the game - when the player presses Return o    r Space or clicks the button
    public void startGame() {
        level = 1;
        loadLevel(level);
        inGame = true;
        background.pause();
    }

    public void next() {
        nifty.gotoScreen("keymap");
    }
    
    public void bind(Nifty nifty, Screen screen) {
        // empty method
    }

    public void onStartScreen() {
        // empty method
    }

    public void onEndScreen() {
        // empty method
    }

    // end the game - when the player clicks the button or presses Return or Space
    public void quitGame() {
        app.stop();
        System.out.println("Thank you for playing. Have a good day!");
    }

    // get current level
    public int getLevel() {
        return level;
    }
}
