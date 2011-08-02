/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class RowReorderChange implements Change {
    final protected List<Integer> _rowIndices;
    
    public RowReorderChange(List<Integer> rowIndices) {
        _rowIndices = rowIndices;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
        	List<Row> oldRows = project.rows;
        	List<Row> newRows = new ArrayList<Row>(oldRows.size());
        	
        	for (Integer oldIndex : _rowIndices) {
        		newRows.add(oldRows.get(oldIndex));
        	}
            
        	project.rows.clear();
        	project.rows.addAll(newRows);
            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
        	int count = project.rows.size();
        	
        	List<Row> newRows = project.rows;
        	List<Row> oldRows = new ArrayList<Row>(count);
        	
        	for (int r = 0; r < count; r++) {
        		oldRows.add(null);
        	}
        	
        	for (int newIndex = 0; newIndex < count; newIndex++) {
        		int oldIndex = _rowIndices.get(newIndex);
        		Row row = newRows.get(newIndex);
        		oldRows.set(oldIndex, row);
        	}
            
        	project.rows.clear();
        	project.rows.addAll(oldRows);
            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("rowIndexCount="); writer.write(Integer.toString(_rowIndices.size())); writer.write('\n');
        for (Integer index : _rowIndices) {
            writer.write(index.toString());
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Integer> rowIndices = null;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            
            if ("rowIndexCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                
                rowIndices = new ArrayList<Integer>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        rowIndices.add(Integer.parseInt(line));
                    }
                }
            }
        }
        
        RowReorderChange change = new RowReorderChange(rowIndices);
        
        return change;
    }
}
