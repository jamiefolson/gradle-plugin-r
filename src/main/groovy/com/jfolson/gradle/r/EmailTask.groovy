package com.jfolson.gradle.r

import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException


import org.apache.commons.mail.*;
import org.gradle.util.ConfigureUtil 

class EmailTask extends DefaultTask {
    class EmailAddressExtension{
        String name = null
        String address = null
    }
    def from = new EmailAddressExtension()
    def to = []
    Email emailObj = null
    boolean gmail = false;

    public EmailTask() {
        def thisref = this
        // This looks really goofy, but setting authentication screws with any 
        // previously set properties, which makes breaks the configuration via closure
        // build script calls to email just add configuration steps to the task
        // by adding the user/password authentication after evaluation, you can be
        // sure it will happen before any closure configuration
        project.afterEvaluate {
            doFirst {
                thisref.setup()
            }
        }
    }
    def setup() {
        if (from.address == null){
            throw new GradleException("From email address must be specified")
        }
        emailObj = new SimpleEmail()
        def authuser= ""
        def authpwd = ""
        // Add \n to move gradle status message
        authuser= System.console().readLine('\nEnter username for email:') 
        authpwd= new String(System.console().readPassword('Enter password for email:'))
        emailObj.setAuthenticator(new DefaultAuthenticator(authuser, authpwd));
        emailObj.setFrom(this.from.address, this.from.name);
        to.each {receiver ->
            emailObj.addTo(receiver.address, receiver.name);
        }
        if (gmail){
            setupGmail()
        }
    }
    def setupGmail() {
        emailObj.setSSLCheckServerIdentity(true)
        emailObj.setTLS(true);
        emailObj.setSmtpPort(587);
        emailObj.setDebug(true);
        emailObj.setHostName("smtp.gmail.com");
        emailObj.getMailSession().getProperties().put("mail.smtps.auth", "true");
        emailObj.getMailSession().getProperties().put("mail.debug", "true");
        emailObj.getMailSession().getProperties().put("mail.smtps.port", "587");
        emailObj.getMailSession().getProperties().put("mail.smtps.socketFactory.port", "587");
        emailObj.getMailSession().getProperties().put("mail.smtps.socketFactory.class",   "javax.net.ssl.SSLSocketFactory");
        emailObj.getMailSession().getProperties().put("mail.smtps.socketFactory.fallback", "false");
        emailObj.getMailSession().getProperties().put("mail.smtp.starttls.enable", "true");
    }

    def from(Closure configureClosure){
        ConfigureUtil.configure(configureClosure, from)
    }
 
    def to(Closure configureClosure){
        def newTo = new EmailAddressExtension()
        ConfigureUtil.configure(configureClosure, newTo)
        to.add(newTo)
    }
 
    def email(Closure configureClosure){
        doFirst {
            ConfigureUtil.configure(configureClosure, emailObj)
        }
    }
    @TaskAction
    def send() {
        emailObj.send()
    }
}


