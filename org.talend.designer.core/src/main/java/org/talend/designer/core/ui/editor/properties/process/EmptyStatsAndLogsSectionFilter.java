// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.core.ui.editor.properties.process;

import org.eclipse.jface.viewers.IFilter;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.designer.core.ui.editor.process.ProcessPart;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.EProperties;

/**
 * Section filter. <br/>
 * 
 */
public class EmptyStatsAndLogsSectionFilter implements IFilter {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IFilter#select(java.lang.Object)
     */
    public boolean select(Object toTest) {
        if (toTest instanceof ProcessPart) {
            //if the input is ProcessPart, it means that the Job is open.
            return false;
        }
        if (toTest instanceof RepositoryNode) {
            RepositoryNode node = (RepositoryNode) toTest;
            if (node.getProperties(EProperties.CONTENT_TYPE) == ERepositoryObjectType.PROCESS) {
                // if node is process and is not displayed in the editor , show the EmptyStatsAndLogsTabPropertySection
                if (StatsAndLogsSectionFilter.getProcessPartByRepositoryNode(node) == null) {
                    return true;
                }
            }
        }
        return false;
    }
}
