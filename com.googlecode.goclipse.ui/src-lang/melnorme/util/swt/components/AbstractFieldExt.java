/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.util.swt.components;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Some extended functionality to {@link AbstractField}.
 * 
 */
public abstract class AbstractFieldExt<VALUE> extends AbstractField<VALUE>{
	
	protected String labelText;
	
	public AbstractFieldExt(String labelText) {
		this.labelText = labelText;
	}
	
	@Override
	protected final void createContents(Composite topControl) {
		createContents_do(topControl);
		createContents_layout();
	}
	
	protected abstract void createContents_do(Composite topControl);
	
	protected abstract void createContents_layout();
	
	
	public GridData layout2Controls(Control leftControl, Control lastControl, boolean expandLast) {
		GridLayout layoutData = (GridLayout) lastControl.getParent().getLayout();
		int numColumns = layoutData.numColumns;
		
		leftControl.setLayoutData(GridDataFactory.swtDefaults().create());
		numColumns--;
		if(numColumns == 0) {
			numColumns = 1;
		}
		
		return layoutLastControl(lastControl, numColumns, expandLast);
	}
	
	public GridData layout1Control(Control lastControl) {
		GridLayout layoutData = (GridLayout) lastControl.getParent().getLayout();
		int numColumns = layoutData.numColumns;
		return layoutLastControl(lastControl, numColumns, true);
	}
	
	protected GridData layoutLastControl(Control lastControl, int numColumns, boolean expandLast) {
		GridDataFactory gdf = expandLast ?
				GridDataFactory.fillDefaults() :
				GridDataFactory.swtDefaults();
		GridData gridData = gdf.span(numColumns, 1).create();
		lastControl.setLayoutData(gridData);
		return gridData;
	}
	
}