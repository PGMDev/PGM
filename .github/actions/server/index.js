#!/usr/bin/env node
// A github action that launches a PGM server.

// Entrypoint.
async function entrypoint() {
	var port = await openPort();
	
	spawnServer(port);
	var url = await establishTunnel(port);

    var message = `Connect to **${url}**`;
	console.log(message.replace(/\*/g, ''));
	await submitComment(message);

	await setTimeout(function() {}, 15 * 60 * 1000); // 15 minutes
	return deleteComment();
}

var { context, GitHub } = require('@actions/github');
var action = require('@actions/core');
var github; // A github client.
var repo; // A repository name.
var comment; // A comment.

async function submitComment(message) {
    if (!github) github = new GitHub(process.env.GITHUB_TOKEN);
    if (!repo) repo = process.env.GITHUB_REPOSITORY.split('/');
    return github.issues.createComment({
        owner: repo[0],
        repo: repo[1],
        issue_number: context.payload.issue.number,
        body: message
    }).then(function(resp) {
        comment = resp.data;
    });
}

async function deleteComment() {
    if (!github || !repo || !comment) return;
    return github.issues.deleteComment({
        owner: repo[0],
        repo: repo[1],
        comment_id: comment.id
    });
}

var allocate = require('get-port');

// Find an open port for use.
async function openPort() {
	return allocate();
}

var spawn = require('child_process').spawn;
var pid; // Process of the server.

// Spawn a Minecraft server in the background.
function spawnServer(port) {
	pid = spawn('java', [
    	'-jar', 'target/PGM-Server.jar',
    	'-h', '127.0.0.1', // Bind to localhost.
    	'-o', 'false',     // Set to offline mode.
    	'-p', port,        // Bind to the requested port.
    	'nogui'			   // Non interactive.
    ]);

	pid.stdout.on('data', function(data) {
	    console.log(data.toString());
	});
	pid.stderr.on('data', function(data) {
	    console.log(data.toString());
	});
	pid.on('close', process.exit);
}

// Kill the Minecraft server, if spawned.
function killServer() {
	if (pid) pid.kill();
}

var ngrok = require('ngrok');

// Establish a TCP tunnel with ngrok.com.
// Returns an ip:port pair, eg. 'tcp.0.ngrok.com:3214'.
async function establishTunnel(port) {
	return await ngrok.connect({
		authtoken: process.env.NGROK_TOKEN,
		proto: 'tcp',
		addr: port,
	}).then(function(url) {
	    return url.replace('tcp://', '');
	});
}

// Tear down the TCP tunnel.
async function teardownTunnel() {
	return ngrok.kill();
}

process.on('exit', async function() {
	killServer();
	await teardownTunnel();
});

entrypoint()
	.catch(function(err) {
	    if (action) {
	        action.setFailed(err.message);
	    }
	});
