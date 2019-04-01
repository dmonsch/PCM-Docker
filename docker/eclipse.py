import os
from zipfile import ZipFile
from io import StringIO
from io import BytesIO
import requests
import xml.etree.ElementTree as ET

eclipse_path = "./eclipse"

def uninstallPlugin(artifacts):
    cmd = eclipse_path + " " + r"-application org.eclipse.equinox.p2.director -uninstallIU " + ",".join(artifacts)
    print(os.popen(cmd).read())

def installPlugin(repo, artifacts, updates = []):
    artifacts.extend(updates)
    cmd = eclipse_path + " " + r"-application org.eclipse.equinox.p2.director -repository " + repo + " -installIU " + (",".join(artifacts))
    
    print(os.popen(cmd).read())

def getStructuredArtifacts(repo):
    url = repo + "content.jar"
    u = requests.get(url)
    
    zipdata = BytesIO()
    zipdata.write(u.content)
    
    input_zip = ZipFile(zipdata)
    files = input_zip.namelist()
    
    ret = {}
    
    if (len(files) == 1):
        contentXml = input_zip.read(files[0])
        parsedXml = ET.fromstring(contentXml)
        
        units = parsedXml.iter("unit")
        for unit in units:
            props = unit.find("properties")
            if (props is not None):
                isgroup = props.find("property[@name='org.eclipse.equinox.p2.type.category']")
                if (isgroup is not None):
                    if (isgroup.get("value") == "true"):
                        name = props.find("property[@name='org.eclipse.equinox.p2.name']").get("value")
                        required = unit.find("requires").findall("required")
                        lst = []
                        for req in required:
                            lst.append(req.get("name"))
                        ret[name] = lst
    
    return ret

def installFiltered(repo, data, updates, filt):
    artifs = []
    
    for key in data:
        for obj in data[key]:
            if (filt(key, obj)):
                artifs.append(obj)
    
    return installPlugin(repo, artifs, updates)

eclipse_repo = "http://download.eclipse.org/releases/oxygen/"
palladio_repo = "https://sdqweb.ipd.kit.edu/eclipse/palladiosimulator/nightly/"
palladio_artifacts_grouped = getStructuredArtifacts(palladio_repo)
palladio_updates = ["org.eclipse.ocl.pivot"]

palladio_automation_repo = "https://dmonsch.github.io/Palladio-Addons-ExperimentAutomation/"
palladio_automation_grouped = getStructuredArtifacts(palladio_automation_repo)

print(palladio_automation_grouped)

# palladio
uninstallPlugin(palladio_updates)
installFiltered(palladio_repo + "," + eclipse_repo, palladio_artifacts_grouped, palladio_updates, lambda x, y : True)
uninstallPlugin(["org.palladiosimulator.experimentautomation.application.feature.feature.group", "org.palladiosimulator.experimentautomation.application.feature.source.feature.group"])
installFiltered(palladio_automation_repo + "," + eclipse_repo, palladio_automation_grouped, [], lambda x, y : y == "org.palladiosimulator.experimentautomation.application.feature.feature.group")