package org.codehaus.groovy.grails.servlet.mvc

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*

class ControllersDynamicMethodsTests extends AbstractGrailsMockTests {
	
	
	void onSetUp() {
				gcl.parseClass(
		"""
		class TestController {
		   def list = {}			
		}
		""")
	}
	
	def request
	def response
	
	void runTest(Closure callable) {
		def mockManager = new MockGrailsPluginManager(ga)
		ctx.registerMockBean("manager", mockManager )
		
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")					
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.ControllersGrailsPlugin")

		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		def servletContext =  createMockServletContext()
		springConfig.servletContext = servletContext		
		
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
			
		def appCtx = springConfig.getApplicationContext()
		mockManager.applicationContext = ctx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, ctx)
		mockManager.doDynamicMethods()
		
		this.request = new MockHttpServletRequest()
		this.response = new MockHttpServletResponse()
		try {
			RequestContextHolder.setRequestAttributes( new GrailsWebRequest(
																request,
																response,
																servletContext
														))		
			callable()
		}
		finally {
			RequestContextHolder.setRequestAttributes(null)	
		}
		
	}
	
	void testFlashObject() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.flash.test = "hello"
			
			assertEquals "hello", testCtrl.flash.test			
		}		
	}
	
	void testParamsObject() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.params.test = "hello"
			
			assertEquals "hello", testCtrl.params.test
						
		}		
	}
	
	void testSessionObject() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.session.test = "hello"
			
			assertEquals "hello", testCtrl.session.test			
		}
	}
	
	void testRequestObjects() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			
			assertNotNull(testCtrl.request)
			assertTrue(testCtrl.request instanceof GrailsHttpServletRequest)
			assertNotNull(testCtrl.response)
			assertTrue(testCtrl.response instanceof GrailsHttpServletResponse)
			assertNotNull(testCtrl.servletContext)
		}
	}
	
	void testErrorsObject() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			def errors = new BindException(this,"test")
			testCtrl.errors = errors
			assertEquals errors, testCtrl.errors
		}
	}
	
	void testModelAndViewObject() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			def mav = new ModelAndView("myView",[hello:"world"])
			testCtrl.modelAndView = mav
			assertEquals mav, testCtrl.modelAndView
		}		
	}
	
	// the following tests just test the the method is invoked successfully
	// we test the actual functionally of each method in separate tests (eg. RenderMethodTests)
	void testRedirectMethod() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.redirect(controller:"blah",action:"list")
		}		
	}
	
	void testRenderMethod() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.render "test"
		}
	}
	
	Integer test1
	
	void testBindDataMethod() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			testCtrl.bindData(this, [test1:"1"])
			assertEquals 1, test1
		}
		
	}
	
	void testActionInfoMethods() {
		runTest {
			def testCtrl = ga.getControllerClass("TestController").newInstance()
			
			def webRequest = RequestContextHolder.currentRequestAttributes()
			webRequest.actionName = "action"
			webRequest.controllerName = "contrl"
			
			assertEquals "action", testCtrl.actionName
			assertEquals "contrl", testCtrl.controllerName
			assertEquals "/contrl/action", testCtrl.actionUri
			assertEquals "/contrl", testCtrl.controllerUri
			
		}		
	}
}