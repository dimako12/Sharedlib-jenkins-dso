import groovy.transform.Field
import com.gpn.pipeline.SastFunction

// Defaults
@Field dont_scan_string = "DON'T SCAN" // default string for the first element (branch/tag) of each repo
@Field git_ssh_proto = "ssh" // ADS git access protocal. Must be "ssh"
@Field git_ssh_port = "${env.SL_SAST_ADS_GIT_SSH_PORT}" // ADS git over ssh tcp port. Example: "8081"
@Field git_api_proto = "https" // ADS api access protocol. Must be "https"
@Field git_api_port = "${env.SL_SAST_ADS_API_PORT}" //ADS api tcp port. Example: "8080"
@Field git_base_url = "${env.SL_SAST_ADS_FQDN}" // ADS server fqdn. Example: server.domain.com
@Field git_collection_path = "${env.SL_SAST_ADS_COLLECTION_PATH}" // ADS collection path. Example: "/TFS/SOME_ORG/"
@Field git_project_req_elements = "1000" // How many elements will be returned by an api call (default 100 is too small)
@Field git_repo_url_prefix = "/_git/" // ADS git url path. Must be "/_git/"
@Field git_api_url_prefix = "/_apis/" // ADS api url path. Must be"/_apis/"
@Field git_cred_id = "${env.SL_SAST_GIT_CRED_ID}" // Jenkins credential id with git private key (sshUserPrivateKey)
@Field git_api_cred_id = "${env.SL_SAST_GIT_API_CRED_ID}" // Jenkins credential id with ADS api token (string)
@Field sast_proto = "https" // SAST server access protocol. Must be "https"
@Field sast_base_url = "${env.SL_SAST_SERVER_FQDN}" // SAST server fqdn. Example: server.domain.com
@Field sast_port = "${env.SL_SAST_SERVER_PORT}" //SAST server tcp port. Example: "8443"
@Field sast_generate_pdf_report = true // Allows SAST to return pdf report to jenkins build (Default "true")
@Field sast_password = "${env.SL_SAST_PASS_CRED_ID}" // Jenkins credential id with SAST encrypted password (string)
@Field sast_project_policy_enforce = false // Enforce SAST policy to return error in jenkins build (Default "false") 
@Field sast_filter_pattern = '''!**/_cvs/**/*, !**/.svn/**/*, !**/.hg/**/*, !**/.git/**/*, !**/.bzr/**/*,
        !**/.gitgnore/**/*, !**/.gradle/**/*, !**/.checkstyle/**/*, !**/.classpath/**/*, !**/bin/**/*,
        !**/obj/**/*, !**/backup/**/*, !**/.idea/**/*, !**/*.DS_Store, !**/*.ipr, !**/*.iws,
        !**/*.bak, !**/*.tmp, !**/*.aac, !**/*.aif, !**/*.iff, !**/*.m3u, !**/*.mid, !**/*.mp3,
        !**/*.mpa, !**/*.ra, !**/*.wav, !**/*.wma, !**/*.3g2, !**/*.3gp, !**/*.asf, !**/*.asx,
        !**/*.avi, !**/*.flv, !**/*.mov, !**/*.mp4, !**/*.mpg, !**/*.rm, !**/*.swf, !**/*.vob,
        !**/*.wmv, !**/*.bmp, !**/*.gif, !**/*.jpg, !**/*.png, !**/*.psd, !**/*.tif, !**/*.swf,
        !**/*.jar, !**/*.zip, !**/*.rar, !**/*.exe, !**/*.dll, !**/*.pdb, !**/*.7z, !**/*.gz,
        !**/*.tar.gz, !**/*.tar, !**/*.gz, !**/*.ahtm, !**/*.ahtml, !**/*.fhtml, !**/*.hdm,
        !**/*.hdml, !**/*.hsql, !**/*.ht, !**/*.hta, !**/*.htc, !**/*.htd, !**/*.war, !**/*.ear,
        !**/*.htmls, !**/*.ihtml, !**/*.mht, !**/*.mhtm, !**/*.mhtml, !**/*.ssi, !**/*.stm,
        !**/*.bin,!**/*.lock,!**/*.svg,!**/*.obj,
        !**/*.stml, !**/*.ttml, !**/*.txn, !**/*.xhtm, !**/*.xhtml, !**/*.class, !**/*.iml, !Checkmarx/Reports/*.*,
        !OSADependencies.json, !**/node_modules/**/*'''

// *** Moved to parameters [:] default in main "call" function ***
//@Field is_debug = true // Turn on additional debugging info
//@Field sast_hide_debug = true // Reduce SAST log generations is jenkins console output (Default "true") 
//@Field sast_cac = false // Enables SAST configuration as code (Default "false") 
//@Field sast_incremental = false // Enables SAST incremental scans (Default "false")
//@Field git_project_name = "TEST_PROJECT" // Project ID for search across ADS collections (use \$ at the end of string to allow precise searching. Example: "TEST_PROJECT\$")
// ***************************************************************

def call(String func, Map parameters = [:]) {
    SastFunction sastFunc = new SastFunction(this)

    // Defaults for parameters [:]
    // These may be overridden by user from the shared library function call
    if(parameters.is_debug == null) {
        parameters.is_debug = false // Define default to true if not exists
    }
    if(parameters.sast_hide_debug == null) {
        parameters.sast_hide_debug = true // Define default to true if not exists
    }
    if(parameters.sast_cac == null) {
        parameters.sast_cac = false // Define default to false if not exists
    }
    if(parameters.sast_incremental == null) {
        parameters.sast_incremental = false // Define default to false if not exists
    }
    if(parameters.set_build_name == null) {
        parameters.set_build_name = false // Define default to false if not exists
    }

    switch(func) {
        /*
        Allows to start SAST scan check with fully automated process with predefined list
        of git repository names and branches/tags for your ADS project.

        Available parameters to call function "manualWithParameters":
         - repos_for_scan:
            !Required!
            Provides list of repos/branches/tags for SAST security scanning.
            Must be Map (Dictionary) with "repo": "branch/tag" syntax. Example:

            ''''
            def my_repos = [
                "repo1": "master",
                "repo2": "v1.0.1"
            ]
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos]
            ''''

         - set_build_name:
            !Optional!
            Set currect build name with additional info.
            Must be bool "false/true". If not defined - defaults to "false". Example:

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "set_build_name": true]
            ''''

         - git_project_name:
            !Optional!
            Set project id for searching across ADS collections (use \$ at the end of string to allow precise searching. Example: "TEST_PROJECT\$"). Example:

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "git_project_name": "MY_ADS_PROJECT"]
            ''''

         - sast_hide_debug:
            !Optional!
            Allows to reduce SAST log generations is jenkins console output (Default "true") . Example:

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "sast_hide_debug": false]
            ''''

         - is_debug:
            !Optional!
            Turn on additional debuging info for shared library functions (Output to jenkins console log)

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "is_debug": true]
            ''''

         - sast_cac:
            !Optional!
            Enables SAST configuration as code (Please refer Checkmarx manual:
            https://checkmarx.atlassian.net/wiki/spaces/SD/pages/1457226433/Setting+up+Scans+in+Jenkins)
            Default to false. Example:

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "sast_cac": true]
            ''''

         - sast_incremental:
            !Optional!
            Enables SAST encremental scans. Default to false. Example:

            ''''
            doSastCheck "manualWithParameters", ["repos_for_scan": my_repos, "sast_incremental": true]
            ''''

        */
        case "manualWithParameters":
            withCredentials([sshUserPrivateKey(credentialsId: git_cred_id, keyFileVariable: 'KEY', usernameVariable: 'GIT_USER')]) {
                def git_project_id = ""
                withCredentials([string(credentialsId: git_api_cred_id, variable: 'TOKEN')]) {
                    if(!parameters.git_project_name){
                        withFolderProperties {
                            parameters.git_project_name = "${env.PROJECT_ID}"
                        }
                    }

                    git_project_id = sastFunc.getGitProjects(GIT_USER, 
                                                            git_api_proto, 
                                                            git_api_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            parameters.git_project_name, 
                                                            git_project_req_elements, 
                                                            git_api_url_prefix, 
                                                            TOKEN, 
                                                            parameters.is_debug)
                }
                
                sastFunc.gitCheckoutRepos(parameters.repos_for_scan,
                                                            dont_scan_string, 
                                                            git_cred_id, 
                                                            git_project_id, 
                                                            git_ssh_proto, 
                                                            git_ssh_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            git_repo_url_prefix, 
                                                            GIT_USER,
                                                            func,
                                                            parameters.set_build_name)
            }
            withCredentials([string(credentialsId: git_api_cred_id, variable: 'SAST_PASSWORD')]) {
                sastFunc.doSastScan(parameters.git_project_name, sast_proto,
                                                                sast_base_url, 
                                                                sast_port, 
                                                                sast_generate_pdf_report, 
                                                                SAST_PASSWORD, 
                                                                sast_filter_pattern,
                                                                sast_project_policy_enforce,
                                                                parameters.sast_hide_debug,
                                                                parameters.sast_cac,
                                                                parameters.sast_incremental)
            }
            cleanWs() // Clean project space from sast check artifacts
            break
        case "interactive":
            /*
            Allows to start SAST scan check with interactive jenkins UI promt
            to allow granulary selection of git repository names and branches/tags
            for your ADS project.

            Available parameters to call function "interactive":
            - set_build_name:
                !Optional!
                Set currect build name with additional info.
                Must be bool "false/true". If not defined - defaults to "false". Example:

                ''''
                doSastCheck "interactive", ["set_build_name": true]
                ''''

            - git_project_name:
                !Optional!
                Set project id for searching across ADS collections (use \$ at the end of string to allow precise searching. Example: "TEST_PROJECT\$"). Example:

                ''''
                doSastCheck "interactive", ["git_project_name": "MY_ADS_PROJECT"]
                ''''

            - sast_hide_debug:
                !Optional!
                Allows to reduce SAST log generations is jenkins console output (Default "true") . Example:

                ''''
                doSastCheck "interactive", ["sast_hide_debug": false]
                ''''

            - is_debug:
                !Optional!
                Turn on additional debuging info for shared library functions (Output to jenkins console log)

                ''''
                doSastCheck "interactive", ["is_debug": true]
                ''''

            - sast_cac:
                !Optional!
                Enables SAST configuration as code (Please refer Checkmarx manual:
                https://checkmarx.atlassian.net/wiki/spaces/SD/pages/1457226433/Setting+up+Scans+in+Jenkins)
                Default to false. Example:

                ''''
                doSastCheck "interactive", ["sast_cac": true]
                ''''

            - sast_incremental:
                !Optional!
                Enables SAST encremental scans. Default to false. Example:

                ''''
                doSastCheck "interactive", ["sast_incremental": true]
                ''''
            */
            withCredentials([sshUserPrivateKey(credentialsId: git_cred_id, keyFileVariable: 'KEY', usernameVariable: 'GIT_USER')]) {
                def dynamicParameters = []
                user_input_repo_name = "" // Used later to save repo_name to be able to recreacte map from string if only one repo is founded

                // Search for requested repo
                def git_project_id = ""
                def projects_list = ""
                withCredentials([string(credentialsId: git_api_cred_id, variable: 'TOKEN')]) {
                    if(!parameters.git_project_name){
                        withFolderProperties {
                            parameters.git_project_name = "${env.PROJECT_ID}"
                        }
                    }
                    git_project_id = sastFunc.getGitProjects(GIT_USER, 
                                                            git_api_proto, 
                                                            git_api_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            parameters.git_project_name, 
                                                            git_project_req_elements, 
                                                            git_api_url_prefix, 
                                                            TOKEN, 
                                                            parameters.is_debug)
                
                    // Get all repos for finded repo
                    projects_list = sastFunc.getGitProjectRepos(GIT_USER, 
                                                            git_project_id, 
                                                            git_api_proto, 
                                                            git_api_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            git_api_url_prefix, 
                                                            TOKEN, 
                                                            parameters.is_debug).readLines()
                }
                
                // Get collection of repos/branches/tags
                (dynamicParameters, user_input_repo_name) = sastFunc.dynamicParametersGenerate(projects_list, 
                                                            KEY, GIT_USER, 
                                                            git_project_id, 
                                                            dont_scan_string, 
                                                            git_ssh_proto, 
                                                            git_ssh_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            git_repo_url_prefix)

                // Show dynamic user input
                def userInput = sastFunc.showDynamicInput("Choose branch\\tag for repositories in project ${parameters.git_project_name} for SAST scanning:", dynamicParameters)

                // Check user input for correct map structure
                userInput = sastFunc.checkUserInput(userInput, user_input_repo_name, parameters.is_debug)

                if (parameters.is_debug) {
                    println(userInput);
                }

                // Cheout selected repos/branches/tags
                sastFunc.gitCheckoutRepos(userInput, dont_scan_string, 
                                                            git_cred_id, 
                                                            git_project_id, 
                                                            git_ssh_proto, 
                                                            git_ssh_port, 
                                                            git_base_url, 
                                                            git_collection_path, 
                                                            git_repo_url_prefix, 
                                                            GIT_USER,
                                                            func,
                                                            parameters.set_build_name)
            }
            
            withCredentials([string(credentialsId: git_api_cred_id, variable: 'SAST_PASSWORD')]) {
                sastFunc.doSastScan(parameters.git_project_name, sast_proto, 
                                                                sast_base_url, 
                                                                sast_port, 
                                                                sast_generate_pdf_report, 
                                                                SAST_PASSWORD, 
                                                                sast_filter_pattern,
                                                                sast_project_policy_enforce,
                                                                parameters.sast_hide_debug,
                                                                parameters.sast_cac,
                                                                parameters.sast_incremental)
            }
            cleanWs() // Clean project space from sast check artifacts
            break
    }
}
