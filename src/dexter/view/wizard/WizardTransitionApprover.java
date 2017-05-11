package dexter.view.wizard;


public interface WizardTransitionApprover
{
	// Should return null if approved, error message if denied.
	public String isTransitionApproved(int oldStageIndex, int newStageIndex);
}
