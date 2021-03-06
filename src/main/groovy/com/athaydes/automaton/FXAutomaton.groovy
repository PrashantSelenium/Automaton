package com.athaydes.automaton

import com.athaydes.automaton.selector.AutomatonSelector
import com.athaydes.automaton.selector.ComplexSelector
import com.athaydes.automaton.selector.CompositeFxSelector
import com.athaydes.automaton.selector.FxSelectors
import com.athaydes.automaton.selector.IntersectFxSelector
import com.athaydes.automaton.selector.MatchType
import com.athaydes.automaton.selector.UnionFxSelector
import com.athaydes.internal.Config
import com.athaydes.internal.interceptor.ToFrontInterceptor
import com.sun.javafx.robot.impl.FXRobotHelper
import groovy.util.logging.Slf4j
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.TextInputControl
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.codehaus.groovy.runtime.InvokerHelper

import java.awt.Point
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

import static com.athaydes.automaton.selector.StringSelectors.matchingAny

/**
 *
 * User: Renato
 */
class FXAutomaton extends Automaton<FXAutomaton> {

    private static instance

    /**
     * Get the singleton instance of FXAutomaton, which is lazily created.
     * @return FXAutomaton singleton instance
     */
    static synchronized FXAutomaton getUser() {
        if ( !instance ) instance = new FXAutomaton()
        instance
    }

    protected FXAutomaton() {}

    /**
     * Block until all events in the JavaFX Thread have been processed.
     */
    FXAutomaton waitForFxEvents() {
        FXApp.doInFXThreadBlocking {}
        this
    }

    FXAutomaton clickOn( Node node, Speed speed = DEFAULT ) {
        moveTo( node, speed ).click()
    }

    FXAutomaton clickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        nodes.each { node -> clickOn( node, speed ).pause( pauseBetween ) }
        this
    }

    FXAutomaton doubleClickOn( Node node, Speed speed = DEFAULT ) {
        moveTo( node, speed ).doubleClick()
    }

    FXAutomaton doubleClickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        nodes.each { node -> doubleClickOn( node, speed ).pause( pauseBetween ) }
        this
    }

    FXAutomaton moveTo( Node node, Speed speed = DEFAULT ) {
        moveTo( { centerOf( node ) }, speed )
    }

    FXAutomaton moveToNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        nodes.each { node -> moveTo( node, speed ).pause( pauseBetween ) }
        this
    }

    FXDragOn<FXAutomaton> drag( Node node ) {
        def target = centerOf node
        new FXDragOn( this, target.x, target.y )
    }

    static Point centerOf( Node node ) {
        assert node != null, "Node could not be found"
        def windowPos = getWindowPosition( node )
        def scenePos = getScenePosition( node )

        def boundsInScene = node.localToScene node.boundsInLocal
        def absX = windowPos.x + scenePos.x + boundsInScene.minX
        def absY = windowPos.y + scenePos.y + boundsInScene.minY
        [ ( absX + boundsInScene.width / 2 ).intValue(),
          ( absY + boundsInScene.height / 2 ).intValue() ] as Point
    }

    static Point getWindowPosition( Node node ) {
        new Point( node.scene.window.x.intValue(), node.scene.window.y.intValue() )
    }

	// required because of JavaFX bug: RT-34307
	protected static Point getScenePosition( Node node ) {
		if ( FXApp.@stage?.scene == node.scene ) { // is top-level Scene
			scenePos.x = Math.max( node.scene.x.intValue(), scenePos.x )
			scenePos.y = Math.max( node.scene.y.intValue(), scenePos.y )
			return scenePos
		} else {
			return [ node.scene.x.intValue(), node.scene.y.intValue() ] as Point
		}
	}

    private static scenePos = new Point()

}

@Slf4j
class FXApp extends Application {

    private static Stage stage
    private static stageFuture = new ArrayBlockingQueue<Stage>( 1 )
    private static Application.Parameters params
    private static Application userApp

    static Scene getScene() {
        if ( stage ) stage.scene
        else throw new RuntimeException( "You must initialize FXApp before you can get the Scene" )
    }

    static Stage getStage() {
        if ( stage ) stage
        else throw new RuntimeException( "You must initialize FXApp before you can get the Stage" )
    }

    static Application.Parameters getApplicationParameters() {
        if ( params != null ) params
        else throw new RuntimeException( "You must initialize FXApp before you can get the Parameters" )
    }

    synchronized static Stage initialize( String... args ) {
        if ( !stage && allJavaFXStages.empty ) {
            log.debug 'Initializing FXApp'
            if ( Automaton.isMac() ) System.setProperty( "javafx.macosx.embedded", "true" )
            Thread.start { Application.launch FXApp, args }
            sleep 500
            stage = stageFuture.poll 10, TimeUnit.SECONDS
            assert stage
            stageFuture = null
            if ( !Config.instance.disableBringStageToFront ) {
                initializeToFrontInterceptor()
            }
            doInFXThreadBlocking {
                stage.scene = emptyScene()
            }
        } else {
            initializeIfStageExists()
        }

        if ( stage ) {
            ensureShowing( stage )
            log.debug "Stage now showing!"
            FXAutomaton.getScenePosition( scene.root )
        }
        stage
    }

    static void initializeIfStageExists() {
        isInitialized()
    }

    static synchronized boolean isInitialized() {
        def stageExists = !allJavaFXStages.empty
        if ( stageExists && stage == null ) stage = allJavaFXStages.first()
        stageExists
    }

    static Collection<Stage> getAllJavaFXStages() {
        try {
            FXRobotHelper.stages
        } catch ( NullPointerException npe ) {
            // no Stage has been initialized, JavaFX code throws a nasty NPE
            [ ]
        }
    }

    static void initializeToFrontInterceptor() {
        def interceptor = new ToFrontInterceptor( FXAutomaton )
        InvokerHelper.newInstance().metaRegistry.setMetaClass( FXAutomaton, interceptor )
    }

    private static void ensureShowing( Stage stage ) {
        doInFXThreadBlocking {
            stage.show()
            stage.toFront()
        }
    }

    /**
     * Run the given Runnable in the JavaFX Thread, blocking until the Runnable has run.
     * @param toRun code to run
     * @param timeoutInSeconds maximum time to wait for the Runnable to complete
     * @throws AssertionError if the action does not complete within the timeout.
     */
    static void doInFXThreadBlocking( Runnable toRun, int timeoutInSeconds = 5 ) {
        if ( Platform.isFxApplicationThread() )
            toRun.run()
        else {
            def blockUntilDone = new ArrayBlockingQueue( 1 )
            Platform.runLater { toRun.run(); blockUntilDone << true }
            assert blockUntilDone.poll( timeoutInSeconds, TimeUnit.SECONDS )
        }
    }

    static void startApp( Application app, String... args ) {
        userApp = app
        initialize( args )
        Platform.runLater { app.start stage }
    }

    @Override
    void start( Stage primaryStage ) throws Exception {
        params = getParameters()
        if ( userApp ) {
            if ( userApp instanceof GroovyObject ) {
                userApp.metaClass.getParameters = { params }
            } else try {
                // try to insert parameters into a Java Application using
                // reflection because this is unsafe (uses undocumented internal operation)
                // and the class/method used may not even exist
                // ParametersImpl.registerParameters( userApp, params )
                ReflectionHelper.callMethodIfExists(
                        Class.forName( 'com.sun.javafx.application.ParametersImpl' ),
                        'registerParameters', userApp, params )
            } catch ( Throwable t ) {
                log.info( 'Error trying to set main parameters for user Application: {}', t )
            }
        }
        primaryStage.title = 'FXAutomaton Stage'
        stageFuture.add primaryStage
    }

    private static Scene emptyScene() {
        new Scene( new VBox(), 600, 500 )
    }

}

class FXer extends HasSelectors<Node, FXer> {

    Node root
    def delegate = FXAutomaton.user

    static final Map<String, AutomatonSelector<Node>> DEFAULT_SELECTORS =
            [
                    '#'    : FxSelectors.byId(),
                    '.'    : FxSelectors.byStyleClass(),
                    'type:': FxSelectors.byType(),
                    'text:': FxSelectors.byText()
            ].asImmutable()

    /**
     * Gets a new instance of <code>FXer</code> using the given
     * top-level Node.
     * <br/>
     * The search space is limited to the given Node.
     * @param node top level JavaFX Node to use. If not given, Automaton attempts to locate an existing JavaFX Scene
     * running in the current JVM and uses the root of that Scene if possible.
     * @return a new FXer instance
     * @throws IllegalArgumentException if a Node is not given and cannot be found in the
     * current JVM instance.
     */
    static FXer getUserWith( Node node = null ) {
        if ( !node && FXApp.initialized ) node = FXApp.scene.root
        if ( !node ) throw new IllegalArgumentException( "Unable to create driver as no JavaFX Node has been given" +
                " and no Scene can be found running in the current JVM instance" )
        new FXer( root: node, selectors: DEFAULT_SELECTORS )
    }

    protected FXer() {}

    /**
     * Block until all events in the JavaFX Thread have been processed.
     */
    FXer waitForFxEvents() {
        delegate.waitForFxEvents()
        this
    }

    Node getAt( ComplexSelector selector ) {
        def res = doGetAt( selector, 1 )
        if ( res ) res.first()
        else throw new GuiItemNotFound( "Could not locate ${selector}" )
    }

    List<Node> getAll( ComplexSelector selector, int limit = Integer.MAX_VALUE ) {
        doGetAt( selector, limit )
    }

    FXer clickOn( Node node, Speed speed = DEFAULT ) {
        delegate.clickOn( node, speed )
        this
    }

    FXer clickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        delegate.clickOnNodes( nodes, pauseBetween, speed )
        this
    }

    FXer clickOn( String selector, Speed speed = DEFAULT ) {
        delegate.clickOn( this[ selector ], speed )
        this
    }

    FXer clickOn( Class<? extends Node> cls, Speed speed = DEFAULT ) {
        delegate.clickOn( this[ cls ], speed )
        this
    }

    FXer clickOn( ComplexSelector selector, Speed speed = DEFAULT ) {
        delegate.clickOn( this[ selector ], speed )
        this
    }

    FXer doubleClickOn( Node node, Speed speed = DEFAULT ) {
        moveTo( node, speed ).doubleClick()
    }

    FXer doubleClickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        delegate.doubleClickOnNodes( nodes, pauseBetween, speed )
        this
    }

    FXer doubleClickOn( String selector, Speed speed = DEFAULT ) {
        moveTo( this[ selector ], speed ).doubleClick()
    }

    FXer doubleClickOn( Class<? extends Node> cls, Speed speed = DEFAULT ) {
        delegate.doubleClickOn( this[ cls ], speed )
        this
    }

    FXer doubleClickOn( ComplexSelector selector, Speed speed = DEFAULT ) {
        delegate.doubleClickOn( this[ selector ], speed )
        this
    }

    FXer moveTo( Node node, Speed speed = DEFAULT ) {
        delegate.moveTo( node, speed )
        this
    }

    FXer moveToNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
        delegate.moveToNodes( nodes, pauseBetween, speed )
        this
    }

    FXer moveTo( String selector, Speed speed = DEFAULT ) {
        delegate.moveTo( this[ selector ], speed )
        this
    }

    FXer moveTo( Class<? extends Node> cls, Speed speed = DEFAULT ) {
        delegate.moveTo( this[ cls ], speed )
        this
    }

    FXer moveTo( ComplexSelector selector, Speed speed = DEFAULT ) {
        delegate.moveTo( this[ selector ], speed )
        this
    }

    /**
     * Enters the given text into the currently selected node.
     * @param text
     * @return this
     */
    FXer enterText( String text ) {
        waitForFxEvents()
        sleep 350
        Platform.runLater {
            def focusOwner = getFocusedTextInputControl()
            if ( focusOwner ) {
                focusOwner.text = text
            } else {
                throw new GuiItemNotFound( focusOwner ?
                        'Currently focused Node does not seem to be a text Node' :
                        'Could not find the currently focused Node' )
            }
        }
        this
    }

    FXDragOn<FXer> drag( Node node ) {
        def target = centerOf node
        new FXerDragOn( this, target.x, target.y )
    }

    FXDragOn<FXer> drag( String selector ) {
        doDrag( selector )
    }

    FXDragOn<FXer> drag( Class<? extends Node> selector ) {
        doDrag( selector )
    }

    FXDragOn<FXer> drag( ComplexSelector selector ) {
        doDrag( selector )
    }

    private FXerDragOn doDrag( selector ) {
        def target = centerOf this[ selector ]
        new FXerDragOn( this, target.x, target.y )
    }

    @Override
    List<Node> getAll( String selector ) {
        super.getAll( selector ).grep { !it.class.simpleName.endsWith( "Skin" ) }
                .toList() as List<Node>
    }

    Point centerOf( Node node ) {
        delegate.centerOf( node )
    }

    /**
     * @return the focused TextInputControl if any, null otherwise.
     */
    TextInputControl getFocusedTextInputControl() {
        def matches = getAll matchingAny( 'type:TextArea', 'type:TextField', 'type:PasswordField' )
        for ( match in matches ) {
            if ( match.focused ) return match
        }
        null
    }

    protected List<Node> doGetAt( ComplexSelector selector, int limit = Integer.MAX_VALUE ) {
        def prefixes_queries = selector.queries.collect { ensurePrefixed( it ) }
        def toMapEntries = { String prefix, String query ->
            new MapEntry( selectors[ prefix ], query )
        }

        def selectors_queries = prefixes_queries.collect( toMapEntries )

        def fxSelector = entries2FxSelector( selector.matchType, selectors_queries )
        fxSelector.apply( null, null, root, limit )
    }

    private CompositeFxSelector entries2FxSelector( MatchType type, List<MapEntry> selectors_queries ) {
        switch ( type ) {
            case MatchType.ANY:
                return new UnionFxSelector( selectors_queries )
            case MatchType.ALL:
                return new IntersectFxSelector( selectors_queries )
            default:
                throw new RuntimeException( "Forgot to implement selector of type ${type}" )
        }
    }

}

class FXDragOn<T extends Automaton<? extends Automaton>> extends DragOn<T> {

    protected FXDragOn( T automaton, fromX, fromY ) {
        super( automaton, fromX, fromY )
    }

    T onto( Node node, Speed speed = Automaton.DEFAULT ) {
        def center = FXAutomaton.centerOf( node )
        onto( center.x, center.y, speed )
    }

}

class FXerDragOn extends FXDragOn<FXer> {

    protected FXerDragOn( FXer fxer, fromX, fromY ) {
        super( fxer, fromX, fromY )
    }

    FXer onto( String selector, Speed speed = Automaton.DEFAULT ) {
        onto( automaton[ selector ], speed )
    }

    FXer onto( Class<? extends Node> selector, Speed speed = Automaton.DEFAULT ) {
        onto( automaton[ selector ], speed )
    }

    FXer onto( ComplexSelector selector, Speed speed = Automaton.DEFAULT ) {
        onto( automaton[ selector ], speed )
    }

}
