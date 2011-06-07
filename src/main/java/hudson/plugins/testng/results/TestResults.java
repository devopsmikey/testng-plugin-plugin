package hudson.plugins.testng.results;

import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Represents all the results gathered for a single build (or a single suite,
 * while parsing the test results)
 *
 * @author nullin
 * @author farshidce
 */
public class TestResults extends BaseResult implements Serializable {

   private static final long serialVersionUID = -3491974223665601995L;
   private List<TestResult> testList = new ArrayList<TestResult>();
   private List<MethodResult> passedTests = new ArrayList<MethodResult>();
   private List<MethodResult> failedTests = new ArrayList<MethodResult>();
   private List<MethodResult> skippedTests = new ArrayList<MethodResult>();
   private List<MethodResult> failedConfigurationMethods = new ArrayList<MethodResult>();
   private List<MethodResult> skippedConfigurationMethods = new ArrayList<MethodResult>();
   private int totalTestCount;
   private int passedTestCount;
   private int failedTestCount;
   private int skippedTestCount;
   private int failedConfigurationMethodsCount;
   private int skippedConfigurationMethodsCount;
   private Map<String, PackageResult> packageMap = new HashMap<String, PackageResult>();

   public TestResults(String name) {
      this.name = name;
   }

   public List<MethodResult> getFailedTests() {
      return failedTests;
   }

   public List<MethodResult> getPassedTests() {
      return passedTests;
   }

   public List<MethodResult> getSkippedTests() {
      return skippedTests;
   }

   public List<MethodResult> getFailedConfigs() {
      return failedConfigurationMethods;
   }

   public List<MethodResult> getSkippedConfigs() {
      return skippedConfigurationMethods;
   }

   public List<TestResult> getTestList() {
      return testList;
   }

   public int getTotalTestCount() {
      return totalTestCount;
   }

   public int getPassedTestCount() {
      return passedTestCount;
   }

   public int getFailedTestCount() {
      return failedTestCount;
   }

   public int getSkippedTestCount() {
      return skippedTestCount;
   }

   public int getFailedConfigCount() {
      return failedConfigurationMethodsCount;
   }

   public int getSkippedConfigCount() {
      return skippedConfigurationMethodsCount;
   }

   public Map<String, PackageResult> getPackageMap() {
      return packageMap;
   }

   public Set<String> getPackageNames() {
      return packageMap.keySet();
   }

   /**
    * Adds only the <test>s that already aren't part of the list
    * @param classList
    */
   public void addUniqueTests(List<TestResult> testList) {
      Set<TestResult> tmpSet = new HashSet<TestResult>(this.testList);
      tmpSet.addAll(testList);
      this.testList = new ArrayList<TestResult>(tmpSet);
   }

   public void setOwner(AbstractBuild<?, ?> owner) {
      this.owner = owner;
      for (TestResult _test : testList) {
         _test.setOwner(owner);
      }
      for (String pkg : packageMap.keySet()) {
         packageMap.get(pkg).setOwner(owner);
      }
   }

   //FIXME: seems screwed up
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      TestResults testResults = (TestResults) o;
      return name.equals(testResults.name) && !(owner != null ? !owner.equals(testResults.owner)
            : testResults.owner != null);
   }

   public int hashCode() {
      int result;
      result = (owner != null ? owner.hashCode() : 0);
      result = 31 * result + name.hashCode();
      return result;
   }

   public String toString() {
      return String.format("TestResults {name='%s', totalTests=%d, " +
          "failedTests=%d, skippedTests=%d, failedConfigs=%d, " +
          "skippedConfigs=%d}", name, totalTestCount, failedTestCount,
          skippedTestCount, failedConfigurationMethodsCount,
          skippedConfigurationMethodsCount);
   }

   /**
    * Updates the calculated fields
    */
   public void tally() {
      failedConfigurationMethodsCount = failedConfigurationMethods.size();
      skippedConfigurationMethodsCount = skippedConfigurationMethods.size();
      failedTestCount = failedTests.size();
      passedTestCount = passedTests.size();
      skippedTestCount = skippedTests.size();
      totalTestCount = passedTestCount + failedTestCount + skippedTestCount;

      packageMap.clear();
      for (TestResult _test : testList) {
         for (ClassResult _class : _test.getClassList()) {
            String pkg = _class.getName();
            int lastDot = pkg.lastIndexOf('.');
            if (lastDot == -1) {
               pkg = "No Package";
            } else {
               pkg = pkg.substring(0, lastDot);
            }
            if (packageMap.containsKey(pkg)) {
               List<ClassResult> classResults = packageMap.get(pkg).getClassList();
               boolean classAlreadyAddedToPackage = false;
               for (ClassResult classResult : classResults) {
                  if (classResult.getName().equals(_class.getName())) {
                     //let's merge the testMethods
                     //loop through and don't add them if the name, startTime, endTime and
                     //other fields are identical
                     List<MethodResult> methods = classResult.getTestMethodList();
                     List<MethodResult> _methods = _class.getTestMethodList();
                     for (MethodResult _method : _methods) {
                        boolean _methodAlreadyAdded = false;
                        for (MethodResult method : methods) {
                           if(_method.getName().equals(method.getName()) &&
                                _method.getDuration() == method.getDuration() &&
                                   _method.getStartedAt().equals(method.getStartedAt())) {
                              _methodAlreadyAdded = true;
                              break;
                           }
                        }
                        if(!_methodAlreadyAdded) {
                           classResult.addTestMethod(_method);
                        }
                     }
                     classAlreadyAddedToPackage = true;
                     break;
                  }
               }
               if (!classAlreadyAddedToPackage) {
                  packageMap.get(pkg).getClassList().add(_class);
               }
            } else {
               PackageResult tpkg = new PackageResult(pkg);
               tpkg.getClassList().add(_class);
               tpkg.setParent(this);
               packageMap.put(pkg, tpkg);
            }
         }
      }
      for (String pkg : packageMap.keySet()) {
         packageMap.get(pkg).tally();
      }
   }

   public Object getDynamic(String token,
                            StaplerRequest req,
                            StaplerResponse rsp) {
      return packageMap.get(token);
   }
}
