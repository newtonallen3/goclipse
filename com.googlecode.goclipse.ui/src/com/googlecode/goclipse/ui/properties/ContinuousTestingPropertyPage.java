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
package com.googlecode.goclipse.ui.properties;

import melnorme.lang.ide.ui.dialogs.AbstractProjectPropertyPage;
import melnorme.util.swt.components.AbstractComponentExt;
import melnorme.util.swt.components.IFieldValueListener;
import melnorme.util.swt.components.fields.CheckBoxField;
import melnorme.util.swt.components.fields.SpinnerNumberField;
import melnorme.util.swt.components.fields.TextField;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.googlecode.goclipse.Environment;
import com.googlecode.goclipse.ui.GoUIMessages;

public class ContinuousTestingPropertyPage extends AbstractProjectPropertyPage {
	
	protected ContinuousTestingOptionsBlock ctoBlock; // Can be null
	
	@Override
	protected Control createContents(Composite parent, IProject project) {
		ctoBlock = new ContinuousTestingOptionsBlock(project);
		return ctoBlock.createComponent(parent);
	}
	
	public static class ContinuousTestingOptionsBlock extends AbstractComponentExt {
		
		protected IProject input;
		
		protected CheckBoxField ctEnablement;
		protected TextField testFilesRegex;
		protected SpinnerNumberField testTimeout;
		
		public ContinuousTestingOptionsBlock(IProject input) {
			this.input = input;
		}
		
		@Override
		protected void createTopLevelControlLayout(Composite topControl) {
			topControl.setLayout(new GridLayout(2, false));
		}
		
		@Override
		protected void createContents(Composite topControl) {
			
			Text ctWarning;
			ctWarning = new Text(topControl, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
			ctWarning.setBackground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
			ctWarning.setEditable(false);
			ctWarning.setText(GoUIMessages.AUTOMATIC_UNIT_TESTING_WARNING);
			ctWarning.setLayoutData(gdFillDefaults().grab(true, false).hint(400, 200).span(2, 1).create());
			
			
			ctEnablement = new CheckBoxField("Enable Continuous Testing");
			ctEnablement.createComponentInlined(topControl);
			
			testFilesRegex = new TextField("Test Name Regex:");
			testFilesRegex.createComponentInlined(topControl);
			
			testTimeout = new SpinnerNumberField("Test Max Time (ms):");
			testTimeout.createComponentInlined(topControl);
			testTimeout.setValueMinimum(100).setValueMaximum(360000).setValueIncrement(10);
			
			ctEnablement.addValueChangedListener(new IFieldValueListener() {
				@Override
				public void fieldValueChanged() {
					updateComponentForEnableButtonChange();
				}
			});
		}
		
		@Override
		public void updateComponentFromInput() {
			ctEnablement.setFieldValue(Environment.INSTANCE.getAutoUnitTest(input));
			testFilesRegex.setFieldValue(Environment.INSTANCE.getAutoUnitTestRegex(input));
			testTimeout.setFieldValue(Environment.INSTANCE.getAutoUnitTestMaxTime(input));
		}
		
		protected void updateComponentForEnableButtonChange() {
			boolean ctEnabled = ctEnablement.getFieldValue();
			testFilesRegex.setEnabled(ctEnabled);
			testTimeout.setEnabled(ctEnabled);
		}
		
		public void updateControlFromDefaults() {
			ctEnablement.setFieldValue(false);
			testFilesRegex.setFieldValue(Environment.INSTANCE.getAutoUnitTestRegexDefault());
			testTimeout.setFieldValue(Environment.INSTANCE.getAutoUnitTestMaxTimeDefault());
		}
		
		public void saveConfig() {
			boolean ctEnabled = ctEnablement.getFieldValue();
			Environment.INSTANCE.setAutoUnitTest(input, ctEnabled);
			
			String unitTestRegex = testFilesRegex.getFieldValue();
			Environment.INSTANCE.setAutoUnitTestRegex(input, unitTestRegex);
			
			int maxTime = testTimeout.getFieldValue();
			Environment.INSTANCE.setAutoUnitTestMaxTime(input, maxTime);
		}
		
	}
	
	@Override
	protected void performDefaults() {
		if(ctoBlock != null) {
			ctoBlock.updateControlFromDefaults();
		}
	}
	
	@Override
	public boolean performOk() {
		if(ctoBlock != null) {
			ctoBlock.saveConfig();
		}
		return true;
	}
	
}