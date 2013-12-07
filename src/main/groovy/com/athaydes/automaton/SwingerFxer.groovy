package com.athaydes.automaton

import javafx.scene.Node

import java.awt.*

/**
 *
 * User: Renato
 */
class SwingerFxer extends Automaton<SwingerFxer> {

	final FXer fxer
	final Swinger swinger

	final fxSelector1stChars = [ '.', '#' ].asImmutable()

	/**
	 * Gets a new instance of <code>SwingerFxer</code> using the given
	 * top-level component and node.
	 * <br/>
	 * The search space is limited to the given Component (in Swing) and
	 * Node (in JavaFX).
	 * @param component top level Swing container to use
	 * @param node top level JavaFX Node to use
	 * @return a new SwingerFxer instance
	 */
	static SwingerFxer getUserWith( Component component, Node node ) {
		new SwingerFxer( component, node )
	}

	protected SwingerFxer( Component component, Node node ) {
		swinger = Swinger.getUserWith( component )
		fxer = FXer.getUserWith( node )
	}

	def getAt( String selector ) {
		if ( isJavaFXSelector( selector ) )
			fxer[ selector ]
		else
			swinger[ selector ]
	}

	def <K> K getAt( Class<K> type ) {
		if ( Node.isAssignableFrom( type ) ) fxer[ type ]
		else swinger[ type ]
	}

	Collection getAll( String selector ) {
		if ( isJavaFXSelector( selector ) )
			fxer.getAll( selector )
		else
			swinger.getAll( selector )
	}

	Collection getAll( Class type ) {
		if ( Node.isAssignableFrom( type ) )
			fxer.getAll( type )
		else
			swinger.getAll( type )
	}

	SwingerFxer clickOn( Node node, Speed speed = DEFAULT ) {
		fxer.clickOn( node, speed )
		this
	}

	SwingerFxer clickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
		fxer.clickOnNodes( nodes, pauseBetween, speed )
		this
	}

	SwingerFxer clickOn( Component component, Speed speed = DEFAULT ) {
		swinger.clickOn( component, speed )
		this
	}

	SwingerFxer clickOn( Collection<Component> components, long pauseBetween = 100, Speed speed = DEFAULT ) {
		swinger.clickOn( components, pauseBetween, speed )
		this
	}

	SwingerFxer clickOn( String selector, Speed speed = DEFAULT ) {
		if ( isJavaFXSelector( selector ) )
			fxer.clickOn( selector, speed )
		else
			swinger.clickOn( selector, speed )
		this
	}

	SwingerFxer clickOn( Class cls, Speed speed = DEFAULT ) {
		clickOn( this[ cls ], speed )
		this
	}

	SwingerFxer doubleClickOn( Node node, Speed speed = DEFAULT ) {
		fxer.doubleClickOn( node, speed )
		this
	}

	SwingerFxer doubleClickOnNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
		fxer.doubleClickOnNodes( nodes, pauseBetween, speed )
		this
	}

	SwingerFxer doubleClickOn( Component component, Speed speed = DEFAULT ) {
		swinger.doubleClickOn( component, speed )
		this
	}

	SwingerFxer doubleClickOn( Collection<Component> components, long pauseBetween = 100, Speed speed = DEFAULT ) {
		swinger.doubleClickOn( components, pauseBetween, speed )
		this
	}

	SwingerFxer doubleClickOn( String selector, Speed speed = DEFAULT ) {
		if ( isJavaFXSelector( selector ) )
			fxer.doubleClickOn( selector, speed )
		else
			swinger.doubleClickOn( selector, speed )
		this
	}

	SwingerFxer doubleClickOn( Class cls, Speed speed = DEFAULT ) {
		doubleClickOn( this[ cls ], speed )
		this
	}

	SwingerFxer moveTo( Node node, Speed speed = DEFAULT ) {
		fxer.moveTo( node, speed )
		this
	}

	SwingerFxer moveToNodes( Collection<Node> nodes, long pauseBetween = 100, Speed speed = DEFAULT ) {
		fxer.moveToNodes( nodes, pauseBetween, speed )
		this
	}

	SwingerFxer moveTo( Component component, Speed speed = DEFAULT ) {
		swinger.moveTo( component, speed )
		this
	}

	SwingerFxer moveTo( Collection<Component> components, long pauseBetween = 100, Speed speed = DEFAULT ) {
		swinger.moveTo( components, pauseBetween, speed )
		this
	}

	SwingerFxer moveTo( String selector, Speed speed = DEFAULT ) {
		if ( isJavaFXSelector( selector ) )
			fxer.moveTo( selector, speed )
		else
			swinger.moveTo( selector, speed )
		this
	}

	SwingerFxer moveTo( Class cls, Speed speed = DEFAULT ) {
		moveTo( this[ cls ], speed )
		this
	}

	SwingerFXerDragOn drag( Node node ) {
		def target = centerOf node
		new SwingerFXerDragOn( this, target.x, target.y )
	}

	SwingerFXerDragOn drag( Component component ) {
		def target = SwingAutomaton.centerOf( component )
		new SwingerFXerDragOn( this, target.x, target.y )
	}

	SwingerFXerDragOn drag( String selector ) {
		if ( isJavaFXSelector( selector ) ) {
			drag( fxer.root.lookup( selector ) )
		} else {
			def prefix_selector = swinger.ensurePrefixed selector
			drag( swinger.findOnePrefixed( prefix_selector ) )
		}
	}

	Point centerOf( Node node ) {
		fxer.centerOf( node )
	}

	protected boolean isJavaFXSelector( String selector ) {
		selector ? selector[ 0 ] in fxSelector1stChars : false
	}


}

class SwingerFXerDragOn extends DragOn<SwingerFxer> {

	protected SwingerFXerDragOn( SwingerFxer automaton, fromX, fromY ) {
		super( automaton, fromX, fromY )
	}

	SwingerFxer onto( Node node, Speed speed = Automaton.DEFAULT ) {
		new FXerDragOn( automaton.fxer, fromX, fromY )
				.onto( node, speed )
		automaton
	}

	SwingerFxer onto( Component component, Speed speed = Automaton.DEFAULT ) {
		new SwingerDragOn( automaton.swinger, fromX, fromY )
				.onto( component, speed )
		automaton
	}

	SwingerFxer onto( String selector, Speed speed = Automaton.DEFAULT ) {
		if ( automaton.isJavaFXSelector( selector ) )
			new FXerDragOn( automaton.fxer, fromX, fromY )
					.onto( selector, speed )
		else
			new SwingerDragOn( automaton.swinger, fromX, fromY )
					.onto( selector, speed )
		automaton
	}
}

