package org.cpsolver.ifs.assignment.context;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.InheritedAssignment;
import org.cpsolver.ifs.model.Value;
import org.cpsolver.ifs.model.Variable;

public class AssignmentContextHelper {

    /**
     * Returns an assignment context associated with the given object. If there is no 
     * assignment context associated with the object yet, one is created using the
     * {@link HasAssignmentContext#createAssignmentContext(Assignment)} method.
     * @param source given object
     * @param assignment given assignment, also implementing {@link CanHoldContext}
     * @return assignment context associated with the given object and the given assignment
     */
    @SuppressWarnings("unchecked")
    public static <V extends Variable<V, T>, T extends Value<V, T>, C extends AssignmentContext> C getContext(HasAssignmentContext<V, T, C> source, Assignment<V, T> assignment) {
        if (assignment.getIndex() >= 0 && assignment.getIndex() < CanHoldContext.sMaxSize) {
            AssignmentContext[] contexts = ((CanHoldContext)source).getContext();
            if (assignment.getIndex() > 0 && assignment instanceof InheritedAssignment) {
                long version = ((InheritedAssignment<V, T>)assignment).getVersion();
                
                InheritedAssignmentContextHolder.VersionedContext<C> context = (InheritedAssignmentContextHolder.VersionedContext<C>)contexts[assignment.getIndex()];
                if (context == null) {
                    context = new InheritedAssignmentContextHolder.VersionedContext<C>();
                    contexts[assignment.getIndex()] = context;
                }
                
                if (!context.isCurrent(version)) {
                    if (source instanceof CanInheritContext && contexts[0] != null)
                        context.setContent(((CanInheritContext<V, T, C>)source).inheritAssignmentContext(assignment, (C)contexts[0]), version);
                    else
                        context.setContent(source.createAssignmentContext(assignment), version);
                }
                
                return context.getContent();
            } else {
                AssignmentContext context = contexts[assignment.getIndex()];
                if (context == null) {
                    context = source.createAssignmentContext(assignment);
                    contexts[assignment.getIndex()] = context;
                }
                return (C) context;
            }
        }
        return assignment.getAssignmentContext(source.getAssignmentContextReference());
    }
}
