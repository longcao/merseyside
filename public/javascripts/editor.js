var options = {
  basePath: '',
  theme: {
    base: '/assets/stylesheets/epiceditor/base/epiceditor.css',
    preview: '/assets/stylesheets/epiceditor/preview/github.css',
    editor: '/assets/stylesheets/epiceditor/editor/epic-dark.css'
  },
  autogrow: true,
  autogrow: {
    minHeight: 400
  }
};

var editor = new EpicEditor(options).load();

$('#submitPost').click(function(event) {
  event.preventDefault();
  var data = {
    title: $('#title').val(),
    content: editor.exportFile(null, 'html'),
    published: $('#published').is(':checked')
  };

  $.ajax({
    type: 'POST',
    contentType: 'application/json; charset=utf-8',
    url: '/save',
    data: JSON.stringify(data)
  });

});
