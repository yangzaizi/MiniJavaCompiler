package miniJava.ContexualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdentificationTable {
     
	 protected int level;
	 private IdEntry latest;
	 protected boolean userDeclared;
	 protected int errorLevel;
	  public IdentificationTable () {
	    level = 0;
	    latest = null;
	    userDeclared=false;
	  }

	  // Opens a new level in the identification table, 1 higher than the
	  // current topmost level.

	  public void openScope () {

	    level ++;
	  }

	  // Closes the topmost level in the identification table, discarding
	  // all entries belonging to that level.

	  public void closeScope () {

	    IdEntry entry, local;

	    // Presumably, idTable.level > 0.
	    entry = this.latest;
	    while (entry!=null && entry.level == this.level) {
	      local = entry;
	      entry = local.previous;
	    }
	    this.level--;
	    
	    this.latest = entry;
	  }

	  // Makes a new entry in the identification table for the given identifier
	  // and attribute. The new entry belongs to the current level.
	  // duplicated is set to to true if there is already an entry for the
	  // same identifier at the current level.

	  public boolean enter (String id, Declaration attr) {

	    IdEntry entry = this.latest;
	    boolean error=false;

	    if(level>=2){
	    	while(entry!=null){
	    		if(entry.level==1){
	    			break;
	    		}
	    		else if(entry.id.equals(id)){
	    			error=true;
	    			break;
	    		}
	    		else
	    			entry=entry.previous;
	    	}
	    }
	    else if(level==1){
	    	while(entry!=null){
	    		if(entry.level==0){
	    			break;
	    		}
	    		else if(entry.id.equals(id)){
	    			error=true;
	    			
	    			errorLevel=entry.level;
	    			break;
	    		}
	    		else
	    			entry=entry.previous;
	    	}
	    }
	    else{
	    	while(entry!=null){
	    		if(entry.id.equals(id)){
	    			
	    			
	    				error=true;
	    				errorLevel=entry.level;
	    				break;
	    			
	    			
	    		}
	    		else{
	    			entry=entry.previous;
	    		}
	    	}
	    }
	    if(!error){
	    	if( (id.equals("System") || id.equals("_PrintStream") || id.equals("String")) && userDeclared==false){
	    		IdEntry newItem=new IdEntry(id, attr, this.level, this.latest,false);
	    		latest=newItem;
	    	}
	    	else{
	    		IdEntry newItem=new IdEntry(id, attr, this.level, this.latest,true);
	    		latest=newItem;
	    		
	    	}
	    }
	    else{
	    	Checker.hasDuplicate=true;
	    	Checker.hasError=true;
	    	
	    }
	    return error;
	  }

	  // Finds an entry for the given identifier in the identification table,
	  // if any. If there are several entries for that identifier, finds the
	  // entry at the highest level, in accordance with the scope rules.
	  // Returns null iff no entry is found.
	  // otherwise returns the attribute field of the entry found.

	  public Declaration retrieve (String id) {

	    IdEntry entry;
	    Declaration attr = null;
	    boolean searching = true;

	    entry = this.latest;
	    while (searching) {
	      if (entry == null)
	        searching = false;
	      else if (entry.id.equals(id)) {
	        
	        
	        	searching = false;
	        	attr = entry.attr;
	        
	      } else
	        entry = entry.previous;
	    }

	    return attr;
	  }
	  
	  public boolean userDefine(String id){
		  	IdEntry entry;
		    boolean searching = true;

		    entry = this.latest;
		    while (searching) {
		      if (entry == null)
		        searching = false;
		      else if (entry.id.equals(id)) {
		        
		        searching = false;
		      } else
		        entry = entry.previous;
		    }

		    return entry.definedByUser;
		  
	  }
}
