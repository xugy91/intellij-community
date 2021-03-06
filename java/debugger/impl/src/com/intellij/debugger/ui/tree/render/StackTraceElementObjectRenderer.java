// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.ui.tree.render;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.FullValueEvaluatorProvider;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.execution.filters.ExceptionFilter;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
* @author egor
*/
class StackTraceElementObjectRenderer extends CompoundReferenceRenderer implements FullValueEvaluatorProvider {
  private static final Logger LOG = Logger.getInstance(StackTraceElementObjectRenderer.class);

  StackTraceElementObjectRenderer() {
    super("StackTraceElement", null, null);
    setClassName("java.lang.StackTraceElement");
    setEnabled(true);
  }

  @Nullable
  @Override
  public XFullValueEvaluator getFullValueEvaluator(final EvaluationContextImpl evaluationContext, final ValueDescriptorImpl valueDescriptor) {
    return new JavaValue.JavaFullValueEvaluator(DebuggerBundle.message("message.node.navigate"), evaluationContext) {
      @Override
      public void evaluate(@NotNull XFullValueEvaluationCallback callback) {
        Value value = valueDescriptor.getValue();
        ClassType type = ((ClassType)value.type());
        Method toString = type.concreteMethodByName("toString", "()Ljava/lang/String;");
        if (toString != null) {
          try {
            Value res =
              evaluationContext.getDebugProcess().invokeMethod(evaluationContext, (ObjectReference)value, toString, Collections.emptyList());
            if (res instanceof StringReference) {
              callback.evaluated("");
              final String line = ((StringReference)res).value();
              ApplicationManager.getApplication().runReadAction(() -> {
                ExceptionFilter filter = new ExceptionFilter(evaluationContext.getDebugProcess().getSession().getSearchScope());
                Filter.Result result = filter.applyFilter(line, line.length());
                if (result != null) {
                  final HyperlinkInfo info = result.getFirstHyperlinkInfo();
                  if (info != null) {
                    DebuggerUIUtil.invokeLater(() -> info.navigate(valueDescriptor.getProject()));
                  }
                }
              });
            }
          }
          catch (EvaluateException e) {
            LOG.info("Exception while getting stack info", e);
          }
        }
      }

      @Override
      public boolean isShowValuePopup() {
        return false;
      }
    };
  }
}
