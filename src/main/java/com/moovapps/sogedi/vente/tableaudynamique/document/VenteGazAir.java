package com.moovapps.sogedi.vente.tableaudynamique.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.axemble.vdoc.sdk.document.extensions.BaseDocumentExtension;
import com.axemble.vdoc.sdk.exceptions.WorkflowModuleException;
import com.axemble.vdoc.sdk.interfaces.ICatalog;
import com.axemble.vdoc.sdk.interfaces.IContext;
import com.axemble.vdoc.sdk.interfaces.IOrganization;
import com.axemble.vdoc.sdk.interfaces.IProject;
import com.axemble.vdoc.sdk.interfaces.IProperty;
import com.axemble.vdoc.sdk.interfaces.IResource;
import com.axemble.vdoc.sdk.interfaces.IResourceDefinition;
import com.axemble.vdoc.sdk.interfaces.IStorageResource;
import com.axemble.vdoc.sdk.interfaces.IViewController;
import com.axemble.vdoc.sdk.interfaces.IWorkflow;
import com.axemble.vdoc.sdk.interfaces.IOptionList.IOption;
import com.axemble.vdoc.sdk.utils.Logger;

public class VenteGazAir extends BaseDocumentExtension {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(VenteGazAir.class);
	
	protected IContext context = null;
	protected IOrganization organization = null;
	protected IProject project = null;
	protected IWorkflow workflow = null;
	protected ICatalog catalogRef = null;
	protected IResourceDefinition resourceDefinitionSuiviBouteille = null;
	
	protected Map<String,String> optionsOk = null;
	
	@Override
	public boolean onAfterLoad() {
		try
		{
			context = getWorkflowModule().getSysadminContext();
			organization = getDirectoryModule().getOrganization(context, getWorkflowModule().getConfiguration().getStringProperty("com.moovapps.sogedi.vente.organization.name"));
			project = getProjectModule().getProject(context, getWorkflowModule().getConfiguration().getStringProperty("com.moovapps.sogedi.vente.project.name"), organization);
			catalogRef = getWorkflowModule().getCatalog(context, "Referentiels", ICatalog.IType.STORAGE, project);
			resourceDefinitionSuiviBouteille = getWorkflowModule().getResourceDefinition(context, catalogRef, "SuiviBouteille");
			
			String depot = (String) getWorkflowInstance().getParentInstance().getValue("Depot");
			IViewController viewController = getWorkflowModule().getViewController(context, IResource.class);
			
			viewController.addEqualsConstraint("Depot", depot);
			viewController.addEqualsConstraint("TypeBoutSuivi", "Gaz de l'air");
			//viewController.addEqualsConstraint("EtatBoutSuivi", "Vide trait�e");
			
			viewController.setOrderBy("DateSuivi", Date.class, false);
			
			Collection<IStorageResource> storageResources = viewController.evaluate(resourceDefinitionSuiviBouteille);
			if(storageResources != null && !storageResources.isEmpty())
			{
				Map<String,String> listeNumero = new HashMap<>();
				String numero = null;
				for(IStorageResource storageResource : storageResources)
				{
					if(storageResource != null)
					{
						
						numero = (String)storageResource.getValue("NumeroBoutSuivi"); 
						if(numero != null && !listeNumero.containsValue(numero))
						{
							listeNumero.put((String) storageResource.getValue("sys_Reference"),numero);
						}
					}
					
				}
				if(listeNumero != null)
				{ 
					int statut = 0;
					ArrayList<IOption> options = new ArrayList<IOption>();
					optionsOk = new HashMap<>();
					for(Entry<String, String> dataEntry : listeNumero.entrySet())
					{
						statut = getStatus(dataEntry.getKey());
						if(statut == 1)
						{
							options.add(getWorkflowModule().createListOption(dataEntry.getValue(),dataEntry.getValue()));
							optionsOk.put(dataEntry.getKey(),dataEntry.getValue());
						}
					}
					if(options != null && options.size() > 0)
					{
						getWorkflowInstance().setList("Numero", options);
					}
					else
					{
						getResourceController().inform("Numero", "Aucune bouteille n'est pr�te pour une vente.");
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("Erreur dans la classe VenteGazAir methode AfterLoad() "+e.getClass()+"-"+e.getMessage());
		}
		return super.onAfterLoad();
	}
	
	@Override
	public boolean isOnChangeSubscriptionOn(IProperty property) {
		if(property.getName().equals("Numero"))
		{
			return true;
		}
		return super.isOnChangeSubscriptionOn(property);
	}

	public void onPropertyChanged(IProperty property) 
	{
		try 
		{
			context = getWorkflowModule().getSysadminContext();
			organization = getDirectoryModule().getOrganization(context, getWorkflowModule().getConfiguration().getStringProperty("com.moovapps.sogedi.vente.organization.name"));
			project = getProjectModule().getProject(context, getWorkflowModule().getConfiguration().getStringProperty("com.moovapps.sogedi.vente.project.name"), organization);
			catalogRef = getWorkflowModule().getCatalog(context, "Referentiels", ICatalog.IType.STORAGE, project);
				
			if(property.getName().equals("Numero"))
			{
				String codeBouteille = (String) getWorkflowInstance().getValue("Numero");
				if(codeBouteille != null)
				{
					
							resourceDefinitionSuiviBouteille = getWorkflowModule().getResourceDefinition(context, catalogRef, "SuiviBouteille");
							
							for(Entry<String, String> dataEntry : optionsOk.entrySet())
							{
								if(dataEntry.getValue().equals(codeBouteille))
								{
									getInfosBOUTEILLE(dataEntry.getKey());
									break;
								}
							}
				}
				
			}
		} catch (Exception e) {
			log.error("Erreur dans la classe VenteGazAir m�thode onPropertyChanged: "+e.getClass()+" - "+e.getMessage());
		}
		super.onPropertyChanged(property);
	}
	

	private void getInfosBOUTEILLE(String ref) 
	{
		try
		{
			IViewController viewController = getWorkflowModule().getViewController(context, IResource.class);
			
			viewController.addEqualsConstraint("sys_Reference", ref);
			//viewController.setOrderBy("DateSuivi", Date.class, false);
			
			Collection<IStorageResource> storageResources = viewController.evaluate(resourceDefinitionSuiviBouteille);
			if(storageResources != null && !storageResources.isEmpty())
			{
				IStorageResource storageResource = storageResources.iterator().next();
				
				getWorkflowInstance().setValue("TypeDeGaz_GA", storageResource.getValue("TypeGazSuivi"));
				getWorkflowInstance().setValue("Capacite", storageResource.getValue("CapaciteBoutSuivi"));
				getWorkflowInstance().setValue("PoidsAVide", storageResource.getValue("PoidsAVideSuivi"));
				getWorkflowInstance().setValue("Poids", storageResource.getValue("PoidsSuivi"));
				getWorkflowInstance().setValue("DateDEpreuve", storageResource.getValue("DateSuivi"));
				
			}
			else
			{
				getWorkflowInstance().setValue("TypeDeGaz_GA", null);
				getWorkflowInstance().setValue("Capacite", null);
				getWorkflowInstance().setValue("PoidsAVide", null);
				getWorkflowInstance().setValue("Poids", null);
				getWorkflowInstance().setValue("DateDEpreuve", null);
			}
		}
		catch (Exception e)
		{
			log.error("Erreur dans la classe BouteilleGA methode getInfosBOUTEILLE "+e.getClass()+"-"+e.getMessage());
		}
	}
	
	private int getStatus(String reference) throws WorkflowModuleException
	{
		IViewController controller = getWorkflowModule().getViewController(context,IResource.class);
		
		controller.addEqualsConstraint("sys_Reference", reference);
		Collection<? extends IStorageResource> storageResources = controller.evaluate(resourceDefinitionSuiviBouteille);
		if(storageResources != null && storageResources.size() > 0)
		{
			IStorageResource storageResource = storageResources.iterator().next();
			String etat = (String) storageResource.getValue("EtatBoutSuivi");
			if(etat != null && etat.equals("Remplie"))
			{
				return 1;
			}
		}
		return 0;
	}
}
