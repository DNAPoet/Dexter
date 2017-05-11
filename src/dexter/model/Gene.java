package dexter.model;

import java.util.*;


public class Gene implements java.io.Serializable, Comparable<Gene>, dexter.cluster.NewickNodeNameProvider
{
	private static final long 			serialVersionUID = -2502738116063026505L;
	
	private Study						study;
	private Map<PredefinedSpreadsheetColumnRole, String>		
										roleToSval = new HashMap<PredefinedSpreadsheetColumnRole, String>();  // no exprns
	private Map<String, String>			userDefinedRoleToSval;
	private Vector<Float>				expressions;
	private Vector<Float>				normalizedExpressions;	// after applying norm program

	
	//
	// The svals are extracted as text from the spreadsheets described by the spreadsheet structure.
	//
	public Gene(Study study, String[] svals, SpreadsheetStructure structure) throws IllegalArgumentException
	{
		this.study = study;
		
		userDefinedRoleToSval = new HashMap<String, String>();
		expressions = new Vector<Float>();	
		
		for (int col=0; col<svals.length; col++)
		{
			if (!structure.usesColumn(col))
				continue;
			String sval = svals[col];
			PredefinedSpreadsheetColumnRole role = structure.getRoleForColumnNumber(col);
			if (role == PredefinedSpreadsheetColumnRole.TIMEPOINT)
			{
				try
				{
					expressions.add(new Float(sval));
				}
				catch (NumberFormatException x)
				{
					throw new IllegalArgumentException("Invalid expression value: " + sval);
				}
			}
			else if (role == PredefinedSpreadsheetColumnRole.ID)
			{
				if (getId() != null)
					throw new IllegalArgumentException("Multiple id columns");
				setValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ID, sval);
			}
			else
				roleToSval.put(role, svals[col]);
		}
		
		normalizedExpressions = expressions;
	}
	
	
	// For cloning and debugging.
	public Gene()		{ }
	
	
	public Gene cloneShallow()
	{
		Gene theClone = new Gene();
		theClone.roleToSval = this.roleToSval;
		theClone.userDefinedRoleToSval = this.userDefinedRoleToSval;
		theClone.expressions = this.expressions;
		theClone.normalizedExpressions = this.normalizedExpressions;
		theClone.study = this.study;
		return theClone;
	}
	
	
	public Gene cloneDeep()
	{
		Gene theClone = new Gene();
		theClone.roleToSval = new HashMap<PredefinedSpreadsheetColumnRole, String>(roleToSval);
		theClone.userDefinedRoleToSval = new HashMap<String, String>(userDefinedRoleToSval);
		theClone.expressions = new Vector<Float>(expressions);
		if (normalizedExpressions != null)
			theClone.normalizedExpressions = new Vector<Float>(normalizedExpressions);
		theClone.study = this.study;
		return theClone;
	}
	
	
	public String toString()
	{
		return "Gene: ID= " + getId() + ", NAME=" + getName();
	}
	
	
	public String expressionsToString()
	{
		String s = getName() + ": ";
		Vector<Vector<Float>> vecs = new Vector<Vector<Float>>();
		vecs.add(expressions);
		if (normalizedExpressions != null)
			vecs.add(normalizedExpressions);
		String[] titles = { "Expressions", " Normalized" };
		for (int i=0; i<vecs.size(); i++)
		{
			s += "\n  " + titles[i] + ": ";
			for (Float xpr: vecs.get(i))
				s += xpr + ", ";
		}
		return s;
	}
	
 
	public String getName()
	{
		return getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.NAME);
	}
	
 
	// If no name returns id, which is always present.
	public String getBestAvailableName()
	{
		String best = getName();
		if (best != null  &&  !best.isEmpty())
			return best;
		
		best = getId();
		if (best != null  &&  !best.isEmpty())
			return best;
		
		return "NO NAME";
	}
	
	
	public int compareTo(Gene that)
	{
		String sThis = this.getBestAvailableName();
		assert sThis != null;
		String sThat = that.getBestAvailableName();
		assert sThat != null;
		return sThis.compareTo(sThat);
	}
	
	
	public boolean equals(Object x)
	{
		return this.compareTo((Gene)x) == 0;
	}
	
	
	public String getValueForPredefinedRole(PredefinedSpreadsheetColumnRole role)
	{
		assert roleToSval != null  :  "Null roleToSval";
		return roleToSval.get(role);
	}
	
	
	public String getValueForUserDefinedRole(String role)
	{
		return userDefinedRoleToSval.get(role);
	}
	
	
	public String getValueForRole(SpreadsheetColumnRole role)
	{
		return role.isPredefined()  ?  
			getValueForPredefinedRole(role.getPredefinedRole())  :  
			getValueForUserDefinedRole(role.getUserDefinedRole());
	}
	
	
	public void setValueForPredefinedRole(PredefinedSpreadsheetColumnRole role, String sval)
	{
		roleToSval.put(role, sval);
	}
	
	
	public void setName(String name)
	{
		setValueForPredefinedRole(PredefinedSpreadsheetColumnRole.NAME, name);
	}
	
	
	public void setValueForUserDefinedRole(String role, String sval)
	{
		userDefinedRoleToSval.put(role, sval);
	}
	
	
	public void setValueForRole(SpreadsheetColumnRole role, String sval)
	{
		if (role.isPredefined())
			setValueForPredefinedRole(role.getPredefinedRole(), sval);
		else
			setValueForUserDefinedRole(role.getUserDefinedRole(), sval);
	}
	
	
	public String getId()
	{
		return getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ID);
	}
	
	
	public void setId(String id)
	{
		setValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ID, id);
	}
	
	
	public String getPathway()
	{
		return getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.KEGG_PATHWAY);
	}
	
	
	public String getAnnotation()
	{
		return getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ANNOTATION);
	}
	
	
	public void absorb(Gene that) throws IllegalArgumentException
	{
		assert this.getId().equals(that.getId());
		
		// Absorb roles.
		for (PredefinedSpreadsheetColumnRole role: that.roleToSval.keySet())
		{
			if (role == PredefinedSpreadsheetColumnRole.ID  ||  role == PredefinedSpreadsheetColumnRole.TIMEPOINT)
				continue;
			if (this.roleToSval.containsKey(role))
				throw new IllegalArgumentException("Duplicate role: " + role);
			setValueForPredefinedRole(role, that.getValueForPredefinedRole(role));
		}
		
		// Absorb user-defined roles.
		for (String udRole: that.userDefinedRoleToSval.keySet())
		{
			if (this.userDefinedRoleToSval.containsKey(udRole))
				throw new IllegalArgumentException("Duplicate user-defined role: " + udRole);
			setValueForUserDefinedRole(udRole, that.getValueForUserDefinedRole(udRole));
		}
		
		// Absorb expression data.
		this.expressions.addAll(that.expressions);
	}
	
	
	public Vector<float[]> getTimeAndExpressionPairs(TimeAssignmentMap timeAssignmentMap)
	{		
		assert timeAssignmentMap != null;
		
		Vector<float[]> ret = new Vector<float[]>();
		int n = 0;
		Vector<Float> nominalExpressions = 
			(normalizedExpressions != null)  ?  normalizedExpressions  :  expressions;
		for (Float hr: timeAssignmentMap.values())
		{
			float expr = nominalExpressions.get(n++);
			ret.add(new float[] { hr, expr });
		}		
		return ret;
	}
	
	
	public Study getStudy()
	{
		return study;
	}
	
	
	public void setStudy(Study study)
	{
		this.study = study;
	}
	
	
	public Organism getOrganism()
	{
		assert study != null  :  "null study for gene " + getName();
		assert study.getOrganism() != null  :  "null organism for gene " + getName() + " in study" + study.getName();
		return study.getOrganism();
	}
	
	
	public Vector<Float> getRawExpressions()
	{
		return expressions;
	}
	
	
	// Returns float[3] = { min, mean, max }.
	public float[] getMinMeanMaxExpressions()
	{
		assert expressions != null  :  "null rawExpressions for " + this;
		
		float[] ret = { Float.MAX_VALUE, 0f, -1.0e6f };
		for (float xpr: expressions)
		{
			ret[0] = Math.min(ret[0], xpr);
			ret[1] += xpr;
			ret[2] = Math.max(ret[2], xpr);
		}
		ret[1] /= expressions.size();
		return ret;
	}
	
	
	public boolean isDiel()
	{
		float[] minMeanMax = getMinMeanMaxExpressions();
		assert minMeanMax[0] <= minMeanMax[2];
		return minMeanMax[2] - minMeanMax[0] >= 1f;
	}
	
	
	public boolean isConstitutive()
	{
		return !isDiel();
	}
	
	
	public float getMaxNormalizedExpression()
	{
		assert normalizedExpressions != null;
		
		float ret = -1.0e6f;
		for (Float x: normalizedExpressions)
			ret = Math.max(ret, x);
		return ret;
	}
	
	
	// Multiplies all normalized expressions by f, presumably to scale them to [0-16].
	public void scaleNormalizedExpressions(float scale)
	{
		assert normalizedExpressions != null;
		
		for (int i=0; i<normalizedExpressions.size(); i++)
			normalizedExpressions.set(i, normalizedExpressions.get(i) * scale);
	}
	
	
	public String getNewickName()
	{
		return getId();
	}
	
	
	// Some spreadsheets might indicate no-value in a cell with "-" or "n/a".
	public void nullifyPlaceholderCellValues(Set<String> placeholders)
	{
		for (PredefinedSpreadsheetColumnRole role: roleToSval.keySet())
		{
			// Don't mess with id, might break uniqueness even worse.
			if (role == PredefinedSpreadsheetColumnRole.ID)
				continue;
			if (placeholders.contains(roleToSval.get(role)))
				roleToSval.put(role, null);
		}
		
		for (String role: userDefinedRoleToSval.keySet())
		{
			if (placeholders.contains(roleToSval.get(role)))
				userDefinedRoleToSval.put(role, null);
		}
	}
	
	
	// Good for multiline tooltips.
	public String toHTMLString()
	{
		return  "<html>Organism: " + getOrganism() + "<br>Study: " + getStudy() + "<br>Id: " + getId();
	}
	
	
	static void sop(Object x)
	{
		System.out.println(x);
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
